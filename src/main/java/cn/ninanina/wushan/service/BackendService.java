package cn.ninanina.wushan.service;

/**
 * 定义若干后台任务，包括实时翻译标题和标签、索引建立等
 */
public interface BackendService {

    /**
     * 开启翻译。
     * 后台翻译视频标题和标签，无限次遍历video表和tag表，对中文内容为空的，调用有道api进行翻译，每次翻译之间间隔一定时间。
     */
    void startTranslate();

    /**
     * 开启增量建立索引。索引不做缓存，搜索慢就慢，反正SSD也慢不到哪里去。而内存实在太紧张了。
     * <p>索引项：title、titleZh，需要翻译了才建立索引。
     */
    void startIndexing();

    //停止建索引
    void stopIndexing();
}
