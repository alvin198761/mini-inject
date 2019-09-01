package org.alvin.mini_inject.plugins;

import java.util.List;
import java.util.Map;

public interface MiniPlugin {

    /**
     * 插件名称
     *
     * @return
     */
    String name();

    /**
     * 运行方法
     */
    Map<Class<?>,Object> doRun(List<Class> classes);
}
