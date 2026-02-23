package com.leteam.locked.ui.screens.home

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private data class DemoPost(
    val username: String,
    val location: String,
    val caption: String
)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    onPostClick: () -> Unit,
    // DEMO ONLY: pass these in from your nav host / parent
    hasPosted: Boolean = false,
    lastPostedImageUri: Uri? = null
) {
    val todayFormatted = LocalDate.now()
        .format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))

    val demoPosts = listOf(
        DemoPost("jackiechan", "Kitchener • Strength", "leg day done"),
        DemoPost("gregg_lerock", "Toronto • Run", "quick run before dinner 🏃‍♀️")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = todayFormatted,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(demoPosts) { post ->
            DemoPostCard(
                post = post,
                onPostClick = onPostClick,
                hasPosted = hasPosted,
                lastPostedImageUri = lastPostedImageUri
            )
        }
    }
}

@Composable
private fun DemoPostCard(
    post: DemoPost,
    onPostClick: () -> Unit,
    hasPosted: Boolean,
    lastPostedImageUri: Uri?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.username,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = post.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f) // ✅ square
                    .clip(RoundedCornerShape(18.dp))
            ) {
                if (hasPosted && lastPostedImageUri != null && lastPostedImageUri != Uri.EMPTY) {
                    Image(
                        painter = rememberAsyncImagePainter(lastPostedImageUri),
                        contentDescription = "Posted workout photo",
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF1E2A78),
                                        Color(0xFF5B2B82),
                                        Color(0xFF0E4D92)
                                    )
                                )
                            )
                            .blur(22.dp)
                    )

                    Column(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Post your workout to see others’ posts.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(onClick = onPostClick) {
                            Text("Post")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = post.caption,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}