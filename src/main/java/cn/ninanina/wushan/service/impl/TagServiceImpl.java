package cn.ninanina.wushan.service.impl;

import cn.ninanina.wushan.domain.TagDetail;
import cn.ninanina.wushan.domain.VideoDetail;
import cn.ninanina.wushan.repository.TagRepository;
import cn.ninanina.wushan.service.TagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class TagServiceImpl implements TagService {
    @Autowired
    private TagRepository tagRepository;

    @Override
    public List<TagDetail> hotTags(int count) {
        return tagRepository.findByVideoCountDesc(count);
    }

    @Override
    public long tagCount() {
        return tagRepository.count();
    }

    @Override
    public List<VideoDetail> randomHotVideosForTag(long tagId) {
        return null;
    }
}
