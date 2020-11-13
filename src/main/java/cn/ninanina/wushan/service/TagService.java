package cn.ninanina.wushan.service;

import cn.ninanina.wushan.domain.TagDetail;
import cn.ninanina.wushan.domain.VideoDetail;

import java.util.List;

public interface TagService {
    /**
     * 获取视频数量最多的tag
     */
    List<TagDetail> hotTags(int count);

    long tagCount();

    List<VideoDetail> randomHotVideosForTag(long tagId);

}
