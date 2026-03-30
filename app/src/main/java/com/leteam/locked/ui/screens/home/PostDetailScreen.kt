package com.leteam.locked.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.leteam.locked.ui.components.TodaysWorkoutCheckInCard
import com.leteam.locked.ui.components.UserListBottomSheet
import java.text.SimpleDateFormat
import java.util.Locale

private val postDetailCardShape = RoundedCornerShape(12.dp)
private val postDetailInnerShape = RoundedCornerShape(10.dp)
private val postDetailDarkGrey = androidx.compose.ui.graphics.Color(0xFF424242)
private val postDetailLightGrey = androidx.compose.ui.graphics.Color(0xFF757575)
private val postDetailCardGray = androidx.compose.ui.graphics.Color(0xFFF2F2F2)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: String,
    onBack: () -> Unit,
    onUserClick: (String) -> Unit = {},
    onPostWorkoutClick: () -> Unit = {},
    viewModel: PostDetailViewModel = viewModel()
) {
    val postWithUser by viewModel.postWithUser.collectAsState()
    val viewerUser by viewModel.viewerUser.collectAsState()
    val hasPostedToday = viewerUser?.dailyPostStatus?.hasPostedToday ?: false
    val isContentLocked = !hasPostedToday
    val isLoading by viewModel.isLoading.collectAsState()
    val commentsDrawerOpen by viewModel.commentsDrawerOpen.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val commentsLoading by viewModel.commentsLoading.collectAsState()
    val likesDrawerOpen by viewModel.likesDrawerOpen.collectAsState()
    val likeUsers by viewModel.likeUsers.collectAsState()
    val likesListLoading by viewModel.likesListLoading.collectAsState()

    LaunchedEffect(postId) {
        viewModel.loadPost(postId)
    }

    if (commentsDrawerOpen) {
        CommentsDrawer(
            comments = comments,
            commentsLoading = commentsLoading,
            onDismiss = { viewModel.closeCommentsDrawer() },
            onSendComment = { text ->
                viewModel.addComment(postId, text) { }
            }
        )
    }

    if (likesDrawerOpen) {
        UserListBottomSheet(
            title = "Likes",
            users = likeUsers,
            loading = likesListLoading,
            onDismiss = { viewModel.closeLikesDrawer() },
            onUserClick = { userId ->
                onUserClick(userId)
                viewModel.closeLikesDrawer()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Post", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val pw = postWithUser
            if (pw != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (isContentLocked) {
                        TodaysWorkoutCheckInCard(onPostWorkoutClick = onPostWorkoutClick)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    PostDetailCard(
                        postWithUser = pw,
                        viewModel = viewModel,
                        onUserClick = onUserClick,
                        isBlurred = isContentLocked
                    )
                }
            }
        }
    }
}

@Composable
private fun PostDetailCard(
    postWithUser: PostWithUser,
    viewModel: PostDetailViewModel,
    onUserClick: (String) -> Unit,
    isBlurred: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, postDetailCardShape),
        shape = postDetailCardShape,
        colors = CardDefaults.cardColors(containerColor = postDetailCardGray)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onUserClick(postWithUser.post.ownerUserId) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (postWithUser.ownerPhotoUrl.isNotBlank()) {
                        AsyncImage(
                            model = postWithUser.ownerPhotoUrl,
                            contentDescription = "Profile",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            modifier = Modifier.size(24.dp),
                            tint = postDetailDarkGrey
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = postWithUser.ownerFullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = postDetailDarkGrey
                    )
                    if (postWithUser.ownerDisplayName.isNotBlank()) {
                        val handle = postWithUser.ownerDisplayName.trimStart('@')
                        Text(
                            text = "@$handle",
                            style = MaterialTheme.typography.bodySmall,
                            color = postDetailLightGrey
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = postDetailInnerShape,
                colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                            .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                    ) {
                        AsyncImage(
                            model = postWithUser.post.photoUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp)
                                .then(
                                    if (isBlurred) Modifier.blur(radius = 40.dp) else Modifier
                                ),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                        val postTime = timeFormat.format(postWithUser.post.createdAt)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Daily Workout",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = postDetailDarkGrey
                                )
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = postDetailLightGrey
                                )
                                Text(
                                    text = postTime,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Normal,
                                    color = postDetailDarkGrey
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val currentUserId = viewModel.currentUserId
                                val isLiked = currentUserId != null && postWithUser.post.likes.contains(currentUserId)
                                val likeCount = postWithUser.post.likes.size
                                IconButton(
                                    onClick = { viewModel.toggleLike(postWithUser.post.id) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = if (isLiked) "Unlike" else "Like",
                                        modifier = Modifier.size(22.dp),
                                        tint = if (isLiked) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            postDetailLightGrey
                                        }
                                    )
                                }
                                if (likeCount > 0) {
                                    Text(
                                        text = likeCount.toString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = postDetailLightGrey,
                                        modifier = Modifier.clickable {
                                            viewModel.openLikesDrawer()
                                        }
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.openCommentsDrawer() },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ChatBubbleOutline,
                                        contentDescription = "Comment",
                                        modifier = Modifier.size(22.dp),
                                        tint = postDetailLightGrey
                                    )
                                }
                                if (postWithUser.commentCount > 0) {
                                    Text(
                                        text = postWithUser.commentCount.toString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = postDetailLightGrey
                                    )
                                }
                            }
                        }
                        if (postWithUser.post.caption.isNotBlank()) {
                            Text(
                                text = postWithUser.post.caption,
                                style = MaterialTheme.typography.bodyMedium,
                                color = postDetailLightGrey
                            )
                        }
                    }
                }
            }
        }
    }
}