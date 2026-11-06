package io.github.foundry4j.quarkforge.dict.mapper;

import io.github.foundry4j.quarkforge.dict.dto.DictItemView;
import io.github.foundry4j.quarkforge.dict.entity.DictItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/// ## DictMapper
/// 字典实体 ↔ 视图映射。
///
/// `componentModel = "cdi"` 让编译期生成的实现类成为 `@ApplicationScoped`
/// CDI Bean，业务代码直接 `@Inject` 使用；实现类由父 pom 配置的
/// `mapstruct-processor` 注解处理器在编译期生成（`target/generated-sources/annotations`），
/// 运行时零反射，可被 GraalVM 原生编译。
@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface DictMapper {

    /// 字典项实体 → 对外视图（记录类型 [DictItemView] 走构造器映射）。
    ///
    /// 字段名不一致的两处显式声明，其余同名字段（`label`、`parentValue`）自动映射。
    @Mapping(source = "itemValue", target = "value")
    @Mapping(source = "sortOrder", target = "sort")
    DictItemView toView(DictItem item);
}
