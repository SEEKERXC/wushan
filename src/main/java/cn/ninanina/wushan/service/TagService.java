package cn.ninanina.wushan.service;

import cn.ninanina.wushan.domain.TagDetail;
import cn.ninanina.wushan.domain.VideoDetail;
import cn.ninanina.wushan.repository.TagRepository;

import javax.annotation.Nonnull;
import java.util.List;

public interface TagService {
    /**
     * 获取视频数量最多的tag
     */
    List<TagDetail> hotTags(int count);

    long tagCount();

    /**
     * 获取标签下的随机热门视频
     *
     * @param tagId 标签id
     */
    List<VideoDetail> getVideosForTag(TagDetail tag, int offset, int limit, String sort);

    /**
     * 获取以首字母开头的标签，包含数字，全小写
     *
     * @param c 首字母，即a-z、0-9
     */
    List<TagDetail> getTagsStartWith(char c,int page, int size);

    /**
     * 搜索标签建议，默认返回10个建议
     */
    List<TagDetail> suggestTag(@Nonnull String word);

    /**
     * 搜索标签
     */
    List<TagDetail> searchForTag(@Nonnull String query, @Nonnull Integer offset, @Nonnull Integer limit);

}
