package com.leteam.locked.ui.screens.profile

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import coil.compose.AsyncImage
import com.leteam.locked.ui.components.UserListBottomSheet
import com.leteam.locked.posts.Post
import com.leteam.locked.workout.Workout
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profileUserId: String? = null,
    onUserClick: (String) -> Unit = {},
    onPostClick: (String) -> Unit = {},
    onBackClick: (() -> Unit)? = null,
    onEditProfileClick: (() -> Unit)? = null,
    viewModel: ProfileViewModel = viewModel()
) {
    val user by viewModel.user.collectAsState()
    val followerCount by viewModel.followerCount.collectAsState()
    val followingCount by viewModel.followingCount.collectAsState()
    val postCount by viewModel.postCount.collectAsState()
    val isFollowing by viewModel.isFollowing.collectAsState()
    val followInProgress by viewModel.followInProgress.collectAsState()
    val followError by viewModel.followError.collectAsState()
    val recentPosts by viewModel.recentPosts.collectAsState()
    val recentWorkouts by viewModel.recentWorkouts.collectAsState()
    val followerUsers by viewModel.followerUsers.collectAsState()
    val followingUsers by viewModel.followingUsers.collectAsState()
    val followListLoading by viewModel.followListLoading.collectAsState()
    val likesDrawerPostId by viewModel.likesDrawerPostId.collectAsState()
    val likeUsers by viewModel.likeUsers.collectAsState()
    val likesListLoading by viewModel.likesListLoading.collectAsState()

    var showFollowList by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(profileUserId, viewModel.currentUserId) {
        viewModel.loadProfile(profileUserId)
    }

    val scrollState = rememberScrollState()
    var showEditDialog by remember { mutableStateOf(false) }

    val showBack = profileUserId != null && onBackClick != null

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.White,
        topBar = {
            if (showBack) {
                TopAppBar(
                    title = { Text("Profile", fontWeight = FontWeight.Black, color = androidx.compose.ui.graphics.Color.Black) },
                    navigationIcon = {
                        IconButton(onClick = { onBackClick?.invoke() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = androidx.compose.ui.graphics.Color.Black)
                        }
                    },
                    colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.White)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            if (user == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            val u = user!!

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEDEDED)),
                    contentAlignment = Alignment.Center
                ) {
                    if (u.photoUrl.isNotBlank()) {
                        AsyncImage(
                            model = u.photoUrl,
                            contentDescription = "Profile photo",
                            modifier = Modifier.fillMaxWidth().height(88.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile photo",
                            modifier = Modifier.size(44.dp),
                            tint = Color.DarkGray.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = u.fullName.ifBlank { "User" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                val handle = if (u.displayName.isNotBlank()) "@${u.displayName}" else u.email.ifBlank { "" }
                if (handle.isNotBlank()) {
                    Text(
                        text = handle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (u.bio.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = u.bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            when {
                isFollowing == null -> {
                    OutlinedButton(
                        onClick = {
                            val handler = onEditProfileClick
                            if (handler != null) handler() else showEditDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Edit profile")
                    }
                }
                else -> {
                    val following = isFollowing
                    if (following != null) {
                        FollowButton(
                            isFollowing = following,
                            inProgress = followInProgress,
                            onToggle = viewModel::toggleFollow
                        )
                    }
                }
            }

            if (followError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = followError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = androidx.compose.ui.graphics.Color(0xFFE0E0E0)
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (isFollowing == null) {
                Text(
                    text = "Your network",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(count = postCount, label = "Posts")
                StatItem(
                    count = followerCount,
                    label = "Followers",
                    onClick = {
                        viewModel.closeLikesDrawer()
                        viewModel.loadFollowerUsers(u.userId)
                        showFollowList = "followers"
                    }
                )
                StatItem(
                    count = followingCount,
                    label = "Following",
                    onClick = {
                        viewModel.closeLikesDrawer()
                        viewModel.loadFollowingUsers(u.userId)
                        showFollowList = "following"
                    }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = androidx.compose.ui.graphics.Color(0xFFE0E0E0)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Recent posts",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (recentPosts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(androidx.compose.ui.graphics.Color(0xFFEDEDED)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No posts yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                recentPosts.take(5).forEach { post ->
                    PostHistoryItem(
                        post = post,
                        onClick = { onPostClick(post.id) },
                        onLikesClick = if (post.likes.isNotEmpty()) {
                            {
                                showFollowList = null
                                viewModel.openLikesDrawer(post.id)
                            }
                        } else {
                            null
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Workout history",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (recentWorkouts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(androidx.compose.ui.graphics.Color(0xFFEDEDED)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No workouts yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                recentWorkouts.firstOrNull()?.let { workout ->
                    WorkoutHistoryItem(workout = workout)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            confirmButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("OK")
                }
            },
            title = { Text("Edit profile") },
            text = { Text("Profile editing will live here soon.") }
        )
    }

    if (showFollowList != null) {
        val title = if (showFollowList == "followers") "Followers" else "Following"
        val users = if (showFollowList == "followers") followerUsers else followingUsers
        UserListBottomSheet(
            title = title,
            users = users,
            loading = followListLoading,
            onDismiss = { showFollowList = null },
            onUserClick = { userId ->
                onUserClick(userId)
                showFollowList = null
            }
        )
    }

    if (likesDrawerPostId != null) {
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
}

private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

@Composable
private fun PostHistoryItem(
    post: Post,
    onClick: () -> Unit = {},
    onLikesClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(androidx.compose.ui.graphics.Color(0xFFEDEDED))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (post.photoUrl.isNotBlank()) {
            AsyncImage(
                model = post.photoUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            if (post.caption.isNotBlank()) {
                Text(
                    text = post.caption,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = dateFormat.format(post.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (post.likes.isNotEmpty()) {
                    val likeLabel = "${post.likes.size} like${if (post.likes.size == 1) "" else "s"}"
                    if (onLikesClick != null) {
                        Text(
                            text = likeLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable(onClick = onLikesClick)
                        )
                    } else {
                        Text(
                            text = likeLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutHistoryItem(workout: Workout) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(androidx.compose.ui.graphics.Color(0xFFEDEDED))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dateFormat.format(workout.workoutDate),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StatItem(
    count: Int,
    label: String,
    onClick: (() -> Unit)? = null
) {
    val columnContent = @Composable {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    if (onClick != null) {
        Box(modifier = Modifier.clickable(onClick = onClick)) { columnContent() }
    } else {
        columnContent()
    }
}

@Composable
private fun StatItem(
    count: Int,
    label: String
) {
    StatItem(count = count, label = label, onClick = null)
}

@Composable
private fun FollowButton(
    isFollowing: Boolean,
    inProgress: Boolean,
    onToggle: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (inProgress) 0.98f else 1f,
        animationSpec = tween(150), label = "scale"
    )
    val backgroundColor by animateColorAsState(
        targetValue = when {
            inProgress -> Color(0xFFEDEDED)
            isFollowing -> Color(0xFFEDEDED)
            else -> Color.Black
        },
        animationSpec = tween(200), label = "bg"
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            inProgress -> Color.DarkGray
            isFollowing -> Color.Black
            else -> Color.White
        },
        animationSpec = tween(200), label = "content"
    )

    Button(
        onClick = { if (!inProgress) onToggle() },
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor,
            disabledContainerColor = Color(0xFFEDEDED),
            disabledContentColor = Color.DarkGray
        ),
        shape = MaterialTheme.shapes.medium,
        enabled = !inProgress
    ) {
        if (inProgress) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = contentColor
            )
            Spacer(modifier = Modifier.size(8.dp))
        }
        Text(
            text = when {
                inProgress -> if (isFollowing) "Unfollowing…" else "Following…"
                isFollowing -> "Following"
                else -> "Follow"
            }
        )
    }
}