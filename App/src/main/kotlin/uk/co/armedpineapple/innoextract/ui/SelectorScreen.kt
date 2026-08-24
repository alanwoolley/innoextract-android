package uk.co.armedpineapple.innoextract.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uk.co.armedpineapple.innoextract.viewmodels.ExtractionViewModel

enum class StageState {
    Active, Inactive, Complete, Warning
}

@Composable
fun SelectorScreen(
    viewModel: ExtractionViewModel,
    onFileSelected: (Uri) -> Unit,
    onExtractClick: () -> Unit
) {
    val validationResult by viewModel.validationResult.collectAsStateWithLifecycle()
    val targetUri by viewModel.target.collectAsStateWithLifecycle()

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.onFileSelected(it)
            onFileSelected(it)
        }
    }

    val directoryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { viewModel.updateTarget(it) }
    }

    val fileStageState = when {
        validationResult == null -> StageState.Active
        validationResult?.isValid == true -> StageState.Complete
        else -> StageState.Warning
    }

    val dirStageState = when {
        fileStageState != StageState.Complete -> StageState.Inactive
        targetUri == null -> StageState.Active
        else -> StageState.Complete
    }

    val extractStageState = when {
        dirStageState != StageState.Complete -> StageState.Inactive
        else -> StageState.Active
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        SelectorStage(
            index = 1,
            title = "Select Installer (.exe)",
            state = fileStageState,
            buttonText = if (validationResult?.isValid == true) "Change File" else "Choose File",
            onButtonClick = { filePicker.launch("*/*") },
            showConnector = true
        )

        SelectorStage(
            index = 2,
            title = "Select Extraction Folder",
            state = dirStageState,
            buttonText = if (targetUri != null) "Change Folder" else "Choose Folder",
            onButtonClick = { directoryPicker.launch(null) },
            showConnector = true
        )

        SelectorStage(
            index = 3,
            title = "Begin Extraction",
            state = extractStageState,
            buttonText = "Start Extracting",
            onButtonClick = onExtractClick,
            showConnector = false
        )
    }
}

@Composable
fun SelectorStage(
    index: Int,
    title: String,
    state: StageState,
    buttonText: String,
    onButtonClick: () -> Unit,
    showConnector: Boolean = false
) {
    val alpha = if (state == StageState.Inactive) 0.38f else 1f
    val containerColor = when (state) {
        StageState.Complete -> Color(0xFFC8FF87)
        StageState.Warning -> MaterialTheme.colorScheme.error
        StageState.Active -> MaterialTheme.colorScheme.primary
        StageState.Inactive -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val contentColor = when (state) {
        StageState.Complete -> Color.Black
        StageState.Warning -> MaterialTheme.colorScheme.onError
        StageState.Active -> MaterialTheme.colorScheme.onPrimary
        StageState.Inactive -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val icon: ImageVector? = when (state) {
        StageState.Complete -> Icons.Default.Check
        StageState.Warning -> Icons.Default.Warning
        else -> null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha),
        verticalAlignment = Alignment.Top
    ) {
        // Vertical Indicator Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(48.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(containerColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = contentColor
                    )
                } else {
                    Text(
                        text = index.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                }
            }
            
            if (showConnector) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(64.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Content Column
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (state == StageState.Active) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            FilledTonalButton(
                onClick = onButtonClick,
                enabled = state != StageState.Inactive,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(buttonText)
            }
            
            // Extra spacer to align with connector line height
            if (showConnector) {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
