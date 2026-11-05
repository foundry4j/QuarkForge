package io.github.foundry4j.quarkforge.excel;

/// 逐行回调；读取过程中不会把整个文件载入内存。
@FunctionalInterface
public interface RowConsumer {

    void accept(RowData row);
}
