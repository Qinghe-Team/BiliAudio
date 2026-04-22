package com.qinghe.biliaudio.core;

import android.content.Context;

import com.qinghe.biliaudio.api.AuthApiClient;
import com.qinghe.biliaudio.api.InteractionApiClient;
import com.qinghe.biliaudio.api.PlayerApiClient;
import com.qinghe.biliaudio.api.SearchApiClient;
import com.qinghe.biliaudio.api.UserApiClient;
import com.qinghe.biliaudio.api.VideoApiClient;
import com.qinghe.biliaudio.auth.AuthRepository;
import com.qinghe.biliaudio.comment.CommentRepository;
import com.qinghe.biliaudio.favorite.FavoriteRepository;
import com.qinghe.biliaudio.history.HistoryRepository;
import com.qinghe.biliaudio.interaction.InteractionRepository;
import com.qinghe.biliaudio.network.BiliHttpClient;
import com.qinghe.biliaudio.player.PlayerController;
import com.qinghe.biliaudio.search.SearchRepository;
import com.qinghe.biliaudio.user.UserRepository;
import com.qinghe.biliaudio.video.VideoRepository;

public class AppContainer {
    public final AuthApiClient authApiClient;
    public final VideoApiClient videoApiClient;
    public final SearchApiClient searchApiClient;
    public final AuthRepository authRepository;
    public final SearchRepository searchRepository;
    public final VideoRepository videoRepository;
    public final UserRepository userRepository;
    public final FavoriteRepository favoriteRepository;
    public final CommentRepository commentRepository;
    public final InteractionRepository interactionRepository;
    public final PlayerController playerController;
    public final HistoryRepository historyRepository;

    public AppContainer(Context context) {
        BiliHttpClient.INSTANCE.init(context);

        authApiClient = new AuthApiClient();
        videoApiClient = new VideoApiClient();
        searchApiClient = new SearchApiClient(videoApiClient);
        UserApiClient userApiClient = new UserApiClient();
        InteractionApiClient interactionApiClient = new InteractionApiClient();
        PlayerApiClient playerApiClient = new PlayerApiClient();

        authRepository = new AuthRepository(authApiClient);
        searchRepository = new SearchRepository(searchApiClient);
        videoRepository = new VideoRepository(videoApiClient);
        userRepository = new UserRepository(userApiClient);
        favoriteRepository = new FavoriteRepository();
        commentRepository = new CommentRepository();
        interactionRepository = new InteractionRepository(interactionApiClient);
        playerController = new PlayerController(context, playerApiClient, videoApiClient);
        historyRepository = new HistoryRepository();
    }
}
