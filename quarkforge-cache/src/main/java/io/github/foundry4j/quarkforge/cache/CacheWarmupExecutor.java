package io.github.foundry4j.quarkforge.cache;

import io.github.foundry4j.quarkforge.cache.annotation.CacheWarmup;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// ## CacheWarmupExecutor
/// [CacheWarmup] 的执行器：应用启动完成后扫描所有 CDI Bean，按 `order` 顺序调用预热方法。
///
/// ```properties
/// quarkforge.cache.warmup.enabled=true      # 全局开关
/// quarkforge.cache.warmup.fail-fast=false   # 失败是否中断启动
/// ```
///
/// 预热方法必须无参，返回值被忽略；调用经由 CDI 代理，因此 `@Cacheable` / `@Transactional`
/// 等拦截器正常生效。默认下单个任务失败只记录错误日志，不影响其余任务与启动流程。
@ApplicationScoped
public class CacheWarmupExecutor {

    private static final Logger LOG = Logger.getLogger(CacheWarmupExecutor.class);

    @Inject
    BeanManager beanManager;

    @Inject
    @ConfigProperty(name = "quarkforge.cache.warmup.enabled", defaultValue = "true")
    boolean enabled;

    @Inject
    @ConfigProperty(name = "quarkforge.cache.warmup.fail-fast", defaultValue = "false")
    boolean failFast;

    void onStart(@Observes StartupEvent event) {
        if (!enabled) {
            LOG.debug("缓存预热已关闭（quarkforge.cache.warmup.enabled=false）");
            return;
        }
        List<WarmupTask> tasks = discoverTasks().stream()
                .filter(task -> task.annotation().enabled())
                .toList();
        if (tasks.isEmpty()) {
            return;
        }

        LOG.infof("开始缓存预热：%d 个任务", tasks.size());
        long start = System.nanoTime();
        int succeeded = 0;

        // StartupEvent 不保证存在请求上下文，预热方法可能依赖 @RequestScoped Bean
        ManagedContext requestContext = Arc.container().requestContext();
        boolean activated = !requestContext.isActive();
        if (activated) {
            requestContext.activate();
        }
        try {
            for (WarmupTask task : tasks) {
                if (invoke(task)) {
                    succeeded++;
                }
            }
        } finally {
            if (activated) {
                requestContext.terminate();
            }
        }

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        LOG.infof("缓存预热结束：成功 %d/%d，耗时 %d ms", succeeded, tasks.size(), elapsedMs);
    }

    /// 扫描所有 Bean，收集标注 [CacheWarmup] 的无参方法，按 `order` 升序排列。
    private List<WarmupTask> discoverTasks() {
        Set<Bean<?>> beans = beanManager.getBeans(Object.class, Any.Literal.INSTANCE);
        List<WarmupTask> tasks = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (Bean<?> bean : beans) {
            Class<?> beanClass = bean.getBeanClass();
            if (beanClass == null) {
                continue;
            }
            for (Method method : warmupMethods(beanClass)) {
                CacheWarmup annotation = method.getAnnotation(CacheWarmup.class);
                if (annotation == null) {
                    continue;
                }
                if (method.getParameterCount() != 0) {
                    LOG.warnf("@CacheWarmup 方法必须无参，已跳过: %s#%s",
                            beanClass.getName(), method.getName());
                    continue;
                }
                String identity = beanClass.getName() + '#' + method.getName()
                        + Arrays.toString(method.getParameterTypes());
                if (seen.add(identity)) {
                    tasks.add(new WarmupTask(annotation, method, bean));
                }
            }
        }
        tasks.sort(Comparator.comparingInt(task -> task.annotation().order()));
        return tasks;
    }

    /// 沿继承链查找标注了 [CacheWarmup] 的方法；子类同签名方法优先。
    private static List<Method> warmupMethods(Class<?> beanClass) {
        Map<String, Method> found = new LinkedHashMap<>();
        for (Class<?> current = beanClass; current != null && current != Object.class;
             current = current.getSuperclass()) {
            Method[] declared;
            try {
                declared = current.getDeclaredMethods();
            } catch (LinkageError e) {
                LOG.warnf(e, "扫描 @CacheWarmup 方法失败: %s", current.getName());
                continue;
            }
            for (Method method : declared) {
                if (!method.isAnnotationPresent(CacheWarmup.class)) {
                    continue;
                }
                String signature = method.getName() + Arrays.toString(method.getParameterTypes());
                found.putIfAbsent(signature, method);
            }
        }
        return List.copyOf(found.values());
    }

    /// 通过 CDI 引用调用预热方法，失败时按 `fail-fast` 决定是否中断启动。
    private boolean invoke(WarmupTask task) {
        CreationalContext<?> creationalContext = beanManager.createCreationalContext(task.bean());
        try {
            Object target = beanManager.getReference(task.bean(), task.bean().getBeanClass(),
                    creationalContext);
            task.method().setAccessible(true);
            task.method().invoke(target);
            LOG.infof("缓存预热成功: %s", task.displayName());
            return true;
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            return handleFailure(task, cause);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return handleFailure(task, e);
        } finally {
            creationalContext.release();
        }
    }

    private boolean handleFailure(WarmupTask task, Throwable cause) {
        if (failFast) {
            throw new IllegalStateException("缓存预热失败: " + task.displayName(), cause);
        }
        LOG.errorf(cause, "缓存预热失败: %s（继续执行后续任务）", task.displayName());
        return false;
    }

    private record WarmupTask(CacheWarmup annotation, Method method, Bean<?> bean) {

        String displayName() {
            if (!annotation.name().isBlank()) {
                return annotation.name();
            }
            return bean.getBeanClass().getSimpleName() + '#' + method.getName();
        }
    }
}
