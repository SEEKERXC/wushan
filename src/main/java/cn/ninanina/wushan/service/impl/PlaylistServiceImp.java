package cn.ninanina.wushan.service.impl;

import cn.ninanina.wushan.domain.Playlist;
import cn.ninanina.wushan.domain.User;
import cn.ninanina.wushan.domain.VideoDetail;
import cn.ninanina.wushan.repository.UserRepository;
import cn.ninanina.wushan.repository.PlaylistRepository;
import cn.ninanina.wushan.service.PlaylistService;
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
        playlistRepository.deleteById(id);
        log.info("removed dir, id:{}", id);
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
        List<VideoDetail> collectedVideos = playlist.getCollectedVideos();
        if (CollectionUtils.isEmpty(collectedVideos)) {
            collectedVideos = new ArrayList<>();
            playlist.setCollectedVideos(collectedVideos);
        }
        //已收藏过
        if (collectedVideos.contains(videoDetail)) {
            return false;
        }
        collectedVideos.add(videoDetail);
        playlist.setUpdateTime(System.currentTimeMillis());
        playlist.setCount(playlist.getCount() + 1);
        playlist.setCover(videoDetail.getCoverUrl());
        log.info("collected video, dir id: {}, video id: {}", playlist.getId(), videoDetail.getId());
        playlistRepository.save(playlist);
        return true;
    }

    @Override
    public Boolean cancelCollect(@Nonnull Playlist playlist, @Nonnull VideoDetail videoDetail) {
        List<VideoDetail> collectedVideos = playlist.getCollectedVideos();
        if (CollectionUtils.isEmpty(collectedVideos) || !collectedVideos.contains(videoDetail)) {
            return false;
        }
        collectedVideos.remove(videoDetail);
        playlist.setCount(playlist.getCount() - 1);
        playlist.setUpdateTime(System.currentTimeMillis());
        if (collectedVideos.size() > 0)
            playlist.setCover(collectedVideos.get(collectedVideos.size() - 1).getCoverUrl());
        playlistRepository.save(playlist);
        log.info("canceled collect video, dir id: {}, video id: {}", playlist.getId(), videoDetail.getId());
        return true;
    }

    @Override
    public List<VideoDetail> listVideos(@Nonnull Long id) {
        Playlist playlist = playlistRepository.findById(id).orElse(null);
        if (playlist == null) return null;
        return playlist.getCollectedVideos();
    }
}
