package cn.ninanina.wushan.service.impl;

import cn.ninanina.wushan.domain.Playlist;
import cn.ninanina.wushan.domain.User;
import cn.ninanina.wushan.domain.VideoDetail;
import cn.ninanina.wushan.repository.UserRepository;
import cn.ninanina.wushan.repository.PlaylistRepository;
import cn.ninanina.wushan.repository.VideoRepository;
import cn.ninanina.wushan.service.PlaylistService;
import cn.ninanina.wushan.service.cache.VideoCacheManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class PlaylistServiceImp implements PlaylistService {
    @Autowired
    private PlaylistRepository playlistRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private VideoRepository videoRepository;
    @Autowired
    private VideoCacheManager videoCacheManager;

    @Override
    public Playlist create(@Nonnull Long userId, @Nonnull String name) {
        User user = userRepository.getOne(userId);
        List<Playlist> playlists = user.getPlaylists();
        if (playlists.size() >= 50) {
            log.warn("user {} wanna create more than 50 dirs, refused.", user.getId());
            return null;
        }
        Playlist playlist = new Playlist();
        playlist.setName(name);
        playlist.setCount(0);
        playlist.setUser(user);
        playlist.setCreateTime(System.currentTimeMillis());
        playlist.setUpdateTime(System.currentTimeMillis());
        playlist.setIsPublic(true);
        playlist = playlistRepository.save(playlist);
        log.info("user created video dir, user id: {}, dir id: {}", user.getId(), playlist.getId());
        return playlist;
    }

    @Override
    public Playlist possess(@Nonnull Long userId, @Nonnull Long dirId) {
        User user = userRepository.getOne(userId);
        List<Playlist> dirs = user.getPlaylists();
        Playlist playlist = playlistRepository.getOne(dirId);
        if (dirs.contains(playlist)) return playlist;
        else return null;
    }

    @Override
    public void delete(@Nonnull Long id) {
        playlistRepository.deleteCollect(id);
        playlistRepository.remove(id);
        playlistRepository.flush();
        log.info("removed playlist, id:{}", id);
    }

    @Override
    public Playlist rename(@Nonnull Long id, @Nonnull String name) {
        Playlist dir = playlistRepository.getOne(id);
        dir.setName(name);
        dir.setUpdateTime(System.currentTimeMillis());
        dir = playlistRepository.save(dir);
        log.info("renamed dir, id: {} new name: {}", id, name);
        return dir;
    }

    @Override
    public List<Playlist> listAll(@Nonnull Long userId) {
        User user = userRepository.getOne(userId);
        return user.getPlaylists();
    }

    @Override
    public Boolean collect(@Nonnull Playlist playlist, @Nonnull VideoDetail videoDetail) {
        //已收藏过
        if (playlistRepository.findCollected(videoDetail.getId(), playlist.getId()) > 0) {
            return false;
        }
        playlistRepository.insertCollect(videoDetail.getId(), playlist.getId());
        log.info("collected video, dir id: {}, video id: {}", playlist.getId(), videoDetail.getId());
        playlist.setUpdateTime(System.currentTimeMillis());
        playlist.setCount(playlist.getCount() + 1);
        playlist.setCover(videoDetail.getCoverUrl());
        playlistRepository.save(playlist);
        videoDetail.setCollected(videoDetail.getCollected() + 1);
        videoCacheManager.saveVideo(videoDetail);
        return true;
    }

    @Override
    public Boolean cancelCollect(@Nonnull Playlist playlist, @Nonnull VideoDetail videoDetail) {
        if (playlistRepository.findCollected(videoDetail.getId(), playlist.getId()) <= 0) {
            return false;
        }
        playlistRepository.deleteCollect(videoDetail.getId(), playlist.getId());
        log.info("canceled collect video, dir id: {}, video id: {}", playlist.getId(), videoDetail.getId());
        playlist.setCount(playlist.getCount() - 1);
        playlist.setUpdateTime(System.currentTimeMillis());
        playlistRepository.save(playlist);
        videoDetail.setCollected(videoDetail.getCollected() - 1);
        videoCacheManager.saveVideo(videoDetail);
        return true;
    }

    @Override
    public List<VideoDetail> listVideos(@Nonnull Long id) {
        Playlist playlist = playlistRepository.findById(id).orElse(null);
        if (playlist == null) return null;
        List<VideoDetail> videoDetails = playlist.getCollectedVideos();
        for (VideoDetail videoDetail : videoDetails) videoCacheManager.loadTagsForVideo(videoDetail);
        return videoDetails;
    }
}
