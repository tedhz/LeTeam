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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onPostClick: () -> Unit,
    onUserClick: (String) -> Unit = {},
    onHeaderProfileClick: () -> Unit = {}
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

    val screenBackground = Color.White
    val listState = rememberLazyListState()
    var headerVisible by remember { mutableStateOf(true) }
    var previousScroll by remember { mutableStateOf(0) }
    var headerHeightPx by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val headerHeightDp = with(density) {
        if (headerHeightPx > 0) headerHeightPx.toDp() else 72.dp
    }

    val headerOffsetY by animateFloatAsState(
        targetValue = if (headerVisible) 0f else -headerHeightPx.coerceAtLeast(1f),
        animationSpec = tween(durationMillis = 250),
        label = "headerOffset"
    )
    val headerContentAlpha by animateFloatAsState(
        targetValue = if (headerVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "headerContentAlpha"
    )

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }
            .map { (index, offset) -> index * 10000 + offset }
            .distinctUntilChanged()
            .collect { scrollPosition ->
                when {
                    scrollPosition <= 0 -> headerVisible = true
                    scrollPosition > previousScroll -> if (headerVisible) headerVisible = false
                    scrollPosition < previousScroll -> if (!headerVisible) headerVisible = true
                }
                previousScroll = scrollPosition
            }
    }

    Box(modifier = Modifier.fillMaxSize().background(screenBackground)) {
        if (isLoading && currentUser == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = headerHeightDp + 8.dp,
                    bottom = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                val hasPostedToday = currentUser?.dailyPostStatus?.hasPostedToday ?: false
                if (!hasPostedToday) {
                    item {
                        TodaysWorkoutCheckInCard(onPostClick = onPostClick)
                    }
                }

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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationY = headerOffsetY
                    alpha = headerContentAlpha
                }
                .onSizeChanged { headerHeightPx = it.height.toFloat() }
                .background(screenBackground)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Locked",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(onClick = onHeaderProfileClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        modifier = Modifier.size(22.dp),
                        tint = Color(0xFF424242)
                    )
                }
            }
        }
    }
}

private val cardGray = Color(0xFFF2F2F2)
private val checkInLockTint = Color(0xFFAA963C)
private val checkInQuestionGrey = Color(0xFF808080)

@Composable
private fun TodaysWorkoutCheckInCard(onPostClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("EEE MMM d", Locale.getDefault())
    val today = dateFormat.format(java.util.Date())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardGray)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = today,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = checkInLockTint
                            )
                            Text(
                                text = "Today's Workout Check-In",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }

                        Text(
                            text = "Have you trained today?",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Normal,
                            color = checkInQuestionGrey
                        )
                    }

                    Button(
                        onClick = onPostClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Post Workout",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

private val outerCardShape = RoundedCornerShape(12.dp)
private val innerCardShape = RoundedCornerShape(10.dp)
private val darkGrey = Color(0xFF424242)
private val lightGrey = Color(0xFF757575)

@Composable
private fun FeedPostCard(
    postWithUser: PostWithUser,
    isBlurred: Boolean,
    viewModel: HomeViewModel,
    onUserClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, outerCardShape),
        shape = outerCardShape,
        colors = CardDefaults.cardColors(containerColor = cardGray)
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
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        modifier = Modifier.size(24.dp),
                        tint = darkGrey
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = postWithUser.ownerFullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = darkGrey
                    )
                    if (postWithUser.ownerDisplayName.isNotBlank()) {
                        val handle = postWithUser.ownerDisplayName.trimStart('@')
                        Text(
                            text = "@$handle",
                            style = MaterialTheme.typography.bodySmall,
                            color = lightGrey
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = innerCardShape,
                colors = CardDefaults.cardColors(containerColor = Color.White)
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
                                    if (isBlurred) {
                                        Modifier.blur(radius = 40.dp)
                                    } else {
                                        Modifier
                                    }
                                ),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Caption 
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
                                    color = darkGrey
                                )
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = lightGrey
                                )
                                Text(
                                    text = postTime,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Normal,
                                    color = darkGrey
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
                                            lightGrey
                                        }
                                    )
                                }
                                if (likeCount > 0) {
                                    Text(
                                        text = likeCount.toString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = lightGrey
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.openCommentsDrawer(postWithUser.post.id) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ChatBubbleOutline,
                                        contentDescription = "Comment",
                                        modifier = Modifier.size(22.dp),
                                        tint = lightGrey
                                    )
                                }
                                if (postWithUser.commentCount > 0) {
                                    Text(
                                        text = postWithUser.commentCount.toString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = lightGrey
                                    )
                                }
                            }
                        }
                        if (postWithUser.post.caption.isNotBlank()) {
                            Text(
                                text = postWithUser.post.caption,
                                style = MaterialTheme.typography.bodyMedium,
                                color = lightGrey
                            )
                        }
                    }
                }
            }
        }
    }
}

