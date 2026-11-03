package io.github.tuyucheng777.quarkforge.data;

import jakarta.enterprise.context.ApplicationScoped;

/// 测试用软删除仓储。
@ApplicationScoped
public class SoftNoteRepository extends SoftDeleteRepository<SoftNote> {
}
