package com.leteam.locked.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onPostClick: () -> Unit,
    onUserClick: (String) -> Unit = {}
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val feedPosts by viewModel.feedPosts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val commentsDrawerPostId by viewModel.commentsDrawerPostId.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val commentsLoading by viewModel.commentsLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadHomeData()
    }

    if (commentsDrawerPostId != null) {
        CommentsDrawer(
            comments = comments,
            commentsLoading = commentsLoading,
            onDismiss = { viewModel.closeCommentsDrawer() },
            onSendComment = { text -> viewModel.addComment(commentsDrawerPostId!!, text) }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Custom header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Locked",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            // Profile picture placeholder
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        
        // Content area
        if (isLoading && currentUser == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Show "Today's Workout Check-in" card if user hasn't posted today
                val hasPostedToday = currentUser?.dailyPostStatus?.hasPostedToday ?: false
                if (!hasPostedToday) {
                    item {
                        TodaysWorkoutCheckInCard(onPostClick = onPostClick)
                    }
                }

                // Feed posts
                items(feedPosts, key = { it.post.id }) { postWithUser ->
                    FeedPostCard(
                        postWithUser = postWithUser,
                        isBlurred = !hasPostedToday,
                        viewModel = viewModel,
                        onUserClick = onUserClick
                    )
                }
            }
        }
    }
}

@Composable
private fun TodaysWorkoutCheckInCard(onPostClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("EEE MMM d", Locale.getDefault())
    val today = dateFormat.format(java.util.Date())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFEDEDED)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = today,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFFFFD700)
                )
                Text(
                    text = "Today's Workout Check-In",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "Have you trained today?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onPostClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Post Workout")
            }
        }
    }
}

@Composable
private fun FeedPostCard(
    postWithUser: PostWithUser,
    isBlurred: Boolean,
    viewModel: HomeViewModel,
    onUserClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFEDEDED)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header with profile picture, name, and username (tappable to open profile)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onUserClick(postWithUser.post.ownerUserId) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Profile picture
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = postWithUser.ownerFullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (postWithUser.ownerDisplayName.isNotBlank()) {
                        Text(
                            text = postWithUser.ownerDisplayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Post image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                AsyncImage(
                    model = postWithUser.post.photoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .then(
                            if (isBlurred) {
                                Modifier.blur(radius = 40.dp)
                            } else {
                                Modifier
                            }
                        ),
                    contentScale = ContentScale.Crop
                )
            }

            // Post content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Workout info with action icons on the same line
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val postTime = timeFormat.format(postWithUser.post.createdAt)
                    
                    Text(
                        text = "Daily Workout • $postTime",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Action icons (like and comment)
                    Row(
                        modifier = Modifier.padding(start = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val currentUserId = viewModel.currentUserId
                        val isLiked = currentUserId != null && postWithUser.post.likes.contains(currentUserId)
                        val likeCount = postWithUser.post.likes.size

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.toggleLike(postWithUser.post.id) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = if (isLiked) "Unlike" else "Like",
                                    modifier = Modifier.size(24.dp),
                                    tint = if (isLiked) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            if (likeCount > 0) {
                                Text(
                                    text = likeCount.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(
                            onClick = { viewModel.openCommentsDrawer(postWithUser.post.id) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Comment,
                                contentDescription = "Comment",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Caption
                if (postWithUser.post.caption.isNotBlank()) {
                    Text(
                        text = postWithUser.post.caption,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatTimeAgo(date: java.util.Date): String {
    val now = System.currentTimeMillis()
    val then = date.time
    val diffMs = now - then
    val diffDays = TimeUnit.MILLISECONDS.toDays(diffMs)
    val diffHours = TimeUnit.MILLISECONDS.toHours(diffMs)
    val diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diffMs)
    return when {
        diffDays > 0 -> "${diffDays}d"
        diffHours > 0 -> "${diffHours}h"
        diffMinutes > 0 -> "${diffMinutes}m"
        else -> "now"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommentsDrawer(
    comments: List<CommentWithAuthor>,
    commentsLoading: Boolean,
    onDismiss: () -> Unit,
    onSendComment: (String) -> Unit
) {
    var commentText by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Comments",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (commentsLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (comments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No comments yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(comments, key = { it.comment.id }) { commentWithAuthor ->
                        CommentRow(commentWithAuthor = commentWithAuthor)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            "Leave a comment.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    maxLines = 1
                )
                IconButton(
                    onClick = {
                        if (commentText.isNotBlank()) {
                            onSendComment(commentText)
                            commentText = ""
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Send,
                        contentDescription = "Send comment",
                        modifier = Modifier.size(24.dp),
                        tint = Color(0xFF424242)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CommentRow(commentWithAuthor: CommentWithAuthor) {
    val c = commentWithAuthor.comment
    val displayName = if (commentWithAuthor.authorDisplayName.isNotBlank()) {
        commentWithAuthor.authorDisplayName
    } else {
        commentWithAuthor.authorFullName
    }
    val nameToShow = displayName.trimStart('@')

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = nameToShow,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatTimeAgo(c.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = c.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
