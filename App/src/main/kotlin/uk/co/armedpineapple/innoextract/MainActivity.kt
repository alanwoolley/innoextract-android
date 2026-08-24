package uk.co.armedpineapple.innoextract

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.palette.graphics.Palette
import com.mikepenz.aboutlibraries.LibsBuilder
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import dagger.hilt.android.AndroidEntryPoint
import uk.co.armedpineapple.innoextract.service.Configuration
import uk.co.armedpineapple.innoextract.service.ExtractCallback
import uk.co.armedpineapple.innoextract.service.ExtractService
import uk.co.armedpineapple.innoextract.service.IExtractService
import uk.co.armedpineapple.innoextract.services.FirstLaunchService
import uk.co.armedpineapple.innoextract.ui.ProgressScreen
import uk.co.armedpineapple.innoextract.ui.SelectorScreen
import uk.co.armedpineapple.innoextract.ui.theme.InnoTheme
import uk.co.armedpineapple.innoextract.viewmodels.ExtractionViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity(), ExtractCallback {

    override fun onProgress(value: Long, max: Long, file: String) {
        val pct = (1.0f * value / max) * 100
        extractionViewModel.updateProgress(pct.toInt())
        extractionViewModel.updateStatus(getString(R.string.extracting) + file)
    }

    override fun onSuccess() {
        extractionViewModel.onComplete()
    }

    override fun onFailure(e: Exception) {
        extractionViewModel.onFail()
    }

    @Inject
    lateinit var firstLaunchService: FirstLaunchService

    private var isServiceBound = false
    private var connection = Connection()
    var launchIntent: Intent? = null

    private val extractionViewModel: ExtractionViewModel by viewModels()

    private val configuration = Configuration(
        showOngoingNotification = false, showFinalNotification = false
    )

    private lateinit var extractService: IExtractService

    inner class Connection : ServiceConnection {
        override fun onServiceDisconnected(className: ComponentName?) {
            Log.d(Companion.LOG_TAG, "Service disconnected")
            isServiceBound = false
        }

        override fun onServiceConnected(className: ComponentName?, binder: IBinder?) {
            Log.d(Companion.LOG_TAG, "Service connected")
            extractService = (binder as ExtractService.ServiceBinder).service
            isServiceBound = true

            launchIntent?.data?.let { uri ->
                extractionViewModel.onFileSelected(uri)
                onFileSelected()
                launchIntent = null
            }
        }
    }

    private fun onFileSelected() {
        if (!isServiceBound) return

        extractionViewModel.fileUri?.let {
            val result = extractService.check(it)
            extractionViewModel.onFileValidated(result, it)
        }
    }

    private fun onExtractButtonPressed(navController: androidx.navigation.NavController) {
        if (!isServiceBound) return
        
        navController.navigate("progress")
        Toast.makeText(this, getString(R.string.extracting_simple), Toast.LENGTH_SHORT).show()
        
        extractService.extract(
            extractionViewModel.fileUri!!, 
            extractionViewModel.target.value!!, 
            this, 
            configuration
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Log.d(Companion.LOG_TAG, "Binding service")
        val i = Intent(this, ExtractService::class.java)
        bindService(i, connection, Context.BIND_ABOVE_CLIENT or Context.BIND_AUTO_CREATE)

        setContent {
            InnoTheme {
                InnoExtractApp(
                    viewModel = extractionViewModel,
                    onFileSelected = { onFileSelected() },
                    onExtractClick = { navController -> onExtractButtonPressed(navController) },
                    onReturnClick = { extractionViewModel.reset() },
                    onOssClick = {
                        LibsBuilder().withLicenseShown(true).withLicenseDialog(true).start(this)
                    },
                    onAboutClick = {
                        val url = "https://www.armedpineapple.co.uk/projects/inno-setup-extractor"
                        val i = Intent(Intent.ACTION_VIEW)
                        i.data = Uri.parse(url)
                        startActivity(i)
                    }
                )
            }
        }

        launchIntent = intent
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isServiceBound) {
            unbindService(connection)
            isServiceBound = false
        }
    }

    companion object {
        private const val LOG_TAG = "MainActivity"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InnoExtractApp(
    viewModel: ExtractionViewModel,
    onFileSelected: (Uri) -> Unit,
    onExtractClick: (androidx.navigation.NavController) -> Unit,
    onReturnClick: () -> Unit,
    onOssClick: () -> Unit,
    onAboutClick: () -> Unit
) {
    val navController = rememberNavController()
    var showMenu by remember { mutableStateOf(false) }
    
    val gogGame by viewModel.gogGame.collectAsStateWithLifecycle()
    val title by viewModel.title.collectAsStateWithLifecycle()
    val subtitle by viewModel.subtitle.collectAsStateWithLifecycle()
    val isComplete by viewModel.isComplete.collectAsStateWithLifecycle()
    val isError by viewModel.isError.collectAsStateWithLifecycle()

    var titleTextColor by remember { mutableStateOf(Color.Unspecified) }
    var bodyTextColor by remember { mutableStateOf(Color.Unspecified) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Softer background surface
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLow))

        // Full screen background image with scrim
        if (gogGame != null) {
            val painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(gogGame?.backgroundImg.toString())
                    .allowHardware(false)
                    .build(),
                onSuccess = { state ->
                    val bitmap = state.result.drawable.toBitmap()
                    val palette = Palette.from(bitmap).generate()
                    palette.dominantSwatch?.let {
                        titleTextColor = Color(it.titleTextColor)
                        bodyTextColor = Color(it.bodyTextColor)
                    }
                }
            )
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Dark scrim to reduce glare and improve text readability
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    title = { Text("Inno Extract") },
                    actions = {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.open_source_software)) },
                                onClick = {
                                    onOssClick()
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.about)) },
                                onClick = {
                                    onAboutClick()
                                    showMenu = false
                                }
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header section
                Spacer(modifier = Modifier.height(20.dp))
                
                AsyncImage(
                    model = gogGame?.logoImg?.toString() ?: R.drawable.ic_logo,
                    contentDescription = "Logo",
                    modifier = Modifier.size(200.dp)
                )

                Text(
                    text = title.ifEmpty { "Inno Extract" },
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (titleTextColor != Color.Unspecified) titleTextColor else MaterialTheme.colorScheme.onSurface
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Text(
                    text = subtitle.ifEmpty { "Inno Setup Installer Extractor" },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        color = if (bodyTextColor != Color.Unspecified) bodyTextColor else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Scrollable content area
                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = "selector",
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable("selector") {
                            SelectorScreen(
                                viewModel = viewModel,
                                onFileSelected = onFileSelected,
                                onExtractClick = { onExtractClick(navController) }
                            )
                        }
                        composable("progress") {
                            ProgressScreen(
                                viewModel = viewModel,
                                onReturnClick = {
                                    onReturnClick()
                                    navController.popBackStack("selector", inclusive = false)
                                }
                            )
                        }
                    }
                }
            }
        }
        
        if (isError) {
            AlertDialog(
                onDismissRequest = { viewModel.reset() },
                title = { Text("Extraction Failed") },
                text = { Text("There was an error extracting the file. Please check the source and target.") },
                confirmButton = {
                    Button(onClick = { 
                        viewModel.reset()
                        navController.popBackStack("selector", inclusive = false)
                    }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}
