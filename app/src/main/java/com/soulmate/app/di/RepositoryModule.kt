package com.soulmate.app.di

import com.soulmate.app.data.repository.AIRepositoryImpl
import com.soulmate.app.data.repository.DiaryRepositoryImpl
import com.soulmate.app.domain.repository.IAIRepository
import com.soulmate.app.domain.repository.IDiaryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.soulmate.app.data.repository.UserRepositoryImpl
import com.soulmate.app.domain.repository.IUserRepository
import com.soulmate.app.data.repository.AuthRepositoryImpl
import com.soulmate.app.domain.repository.IAuthRepository
import com.soulmate.app.data.repository.CommunityRepositoryImpl
import com.soulmate.app.domain.repository.ICommunityRepository
import com.soulmate.app.data.repository.ChatRepositoryImpl
import com.soulmate.app.domain.repository.IChatRepository


@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDiaryRepository(
        diaryRepositoryImpl: DiaryRepositoryImpl
    ): IDiaryRepository

    @Binds
    @Singleton
    abstract fun bindAIRepository(
        aiRepositoryImpl: AIRepositoryImpl
    ): IAIRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): IUserRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): IAuthRepository

    @Binds
    @Singleton
    abstract fun bindCommunityRepository(
        communityRepositoryImpl: CommunityRepositoryImpl
    ): ICommunityRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        chatRepositoryImpl: ChatRepositoryImpl
    ): IChatRepository
}