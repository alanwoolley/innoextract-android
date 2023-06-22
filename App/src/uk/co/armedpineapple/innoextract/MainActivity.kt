package uk.co.armedpineapple.innoextract

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.mikepenz.aboutlibraries.LibsBuilder
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.debug
import uk.co.armedpineapple.innoextract.databinding.ActivityMainBinding
import uk.co.armedpineapple.innoextract.fragments.FileValidationErrorFragment
import uk.co.armedpineapple.innoextract.fragments.IntroFragment
import uk.co.armedpineapple.innoextract.fragments.OnFragmentInteractionListener
import uk.co.armedpineapple.innoextract.fragments.ProgressFragment
import uk.co.armedpineapple.innoextract.fragments.SelectorFragment
import uk.co.armedpineapple.innoextract.gogapi.GogGame
import uk.co.armedpineapple.innoextract.service.Configuration
import uk.co.armedpineapple.innoextract.service.ExtractCallback
import uk.co.armedpineapple.innoextract.service.ExtractService
import uk.co.armedpineapple.innoextract.service.IExtractService
import uk.co.armedpineapple.innoextract.services.FirstLaunchService
import uk.co.armedpineapple.innoextract.viewmodels.ExtractionViewModel
import javax.inject.Inject

class MainActivity : OnFragmentInteractionListener, ExtractCallback, AnkoLogger,
    AppCompatActivity() {

    override fun onProgress(value: Long, max: Long, file: String) {
        val pct = (1.0f * value / max) * 100
        runOnUiThread { extractionViewModel.updateProgress(pct.toInt()) }
        runOnUiThread { extractionViewModel.updateStatus(getString(R.string.extracting) + file) }
    }

    override fun onSuccess() {
        runOnUiThread { extractionViewModel.onComplete() }
    }

    override fun onFailure(e: Exception) {
        runOnUiThread { extractionViewModel.onFail() }
    }

    @Inject
    lateinit var firstLaunchService: FirstLaunchService

    private var isServiceBound = false
    private var connection = Connection()
    var launchIntent: Intent? = null

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var extractionViewModel: ExtractionViewModel

    private val configuration = Configuration(
        showOngoingNotification = false, showFinalNotification = false
    )

    private lateinit var extractService: IExtractService

    private var shouldShowInstructions = false

    private fun showProgressFragment() {
        val bottomFragment = supportFragmentManager.findFragmentById(R.id.bottomFragment)

        if (bottomFragment != null) {
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom)
                .hide(bottomFragment)
                .add(R.id.bottomFragment, ProgressFragment(), PROGRESS_FRAGMENT_TAG)
                .setReorderingAllowed(true).commitAllowingStateLoss()
        }
    }

    private fun showSelectorFragment() {
        val selectorFragment = supportFragmentManager.findFragmentByTag(SELECTOR_FRAGMENT_TAG)
        val progressFragment = supportFragmentManager.findFragmentByTag(PROGRESS_FRAGMENT_TAG)

        var transaction = supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom)

        progressFragment?.let { transaction = transaction.remove(progressFragment) }

        transaction = if (selectorFragment != null) {
            transaction.show(selectorFragment)
        } else {
            transaction.add(R.id.bottomFragment, SelectorFragment(), SELECTOR_FRAGMENT_TAG)
        }

        transaction.setReorderingAllowed(true).commitAllowingStateLoss()
    }

    inner class Connection : ServiceConnection {
        override fun onServiceDisconnected(className: ComponentName?) {
            debug("Service disconnected")
            isServiceBound = false
        }

        override fun onServiceConnected(className: ComponentName?, binder: IBinder?) {
            debug("Service connected")
            extractService = (binder as ExtractService.ServiceBinder).service
            isServiceBound = true

            if (launchIntent != null) {
                val uri = launchIntent?.data
                if (uri != null) {
                    (supportFragmentManager.findFragmentById(R.id.bottomFragment) as? SelectorFragment)?.onNewFile(
                        uri
                    )
                    onFileSelected()
                }
                launchIntent = null
            }
        }
    }


    override fun onFileSelected() {
        if (!isServiceBound) {
            return
        }

        extractionViewModel.fileUri?.let {
            val result = extractService.check(it)
            extractionViewModel.onFileValidated(result, it)
            if (!result.isValid) {
                val errorFragment = FileValidationErrorFragment()
                errorFragment.show(supportFragmentManager, "error")
            }
        }
    }

    override fun onReturnButtonPressed() {
        binding.backgroundImg.setImageDrawable(null)
        binding.iconImage.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_logo))
        showSelectorFragment()
        extractionViewModel.reset()
    }

    override fun onExtractButtonPressed() {
        if (!isServiceBound) {
            return
        }
        showProgressFragment()
        toast(getString(R.string.extracting_simple), Toast.LENGTH_SHORT)
        extractService.extract(
            extractionViewModel.fileUri!!, extractionViewModel.target.value!!, this, configuration
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }

        (application as AndroidApplication).component.inject(this)

        debug("Binding service")
        val i = Intent(this, ExtractService::class.java)
        val serviceConnected =
            bindService(i, connection, Context.BIND_ABOVE_CLIENT or Context.BIND_AUTO_CREATE)
        debug("Service connected? : $serviceConnected")

        extractionViewModel = ViewModelProvider(this)[ExtractionViewModel::class.java]
        extractionViewModel.gogGame.observe(this) { game: GogGame? ->
            if (game != null) {
                val background = binding.backgroundImg
                val logo = binding.iconImage
                Glide.with(this).asBitmap().load(game.backgroundImg.toString())
                    .listener(object : RequestListener<Bitmap> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Bitmap>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            return true
                        }

                        override fun onResourceReady(
                            resource: Bitmap?,
                            model: Any?,
                            target: Target<Bitmap>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            resource?.let {
                                val palette = Palette.Builder(it).generate()
                                binding.title.setTextColor(palette.dominantSwatch!!.titleTextColor)
                                binding.subtitle.setTextColor(palette.dominantSwatch!!.bodyTextColor)
                            }

                            return false
                        }
                    }).into(background)

                Glide.with(this).asBitmap().load(game.logoImg.toString()).into(logo)
            } else {
                binding.backgroundImg.setImageDrawable(null)
                binding.iconImage.setImageDrawable(
                    AppCompatResources.getDrawable(
                        this, R.drawable.ic_logo
                    )
                )
            }
        }
        val binding: ActivityMainBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.lifecycleOwner = this
        binding.vm = extractionViewModel
        _binding = binding

        // Set up the selector fragment as the initial view
        showSelectorFragment()

        binding.bottomFragment.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom -> updateMotion() }
        configureMenu()

        launchIntent = intent
        shouldShowInstructions = firstLaunchService.isFirstLaunch

        updateMotion()
    }

    private fun configureMenu() {
        val ossMenu = binding.toolbar.menu.add(getString(R.string.open_source_software))
        ossMenu.setOnMenuItemClickListener {

            LibsBuilder().withLicenseShown(true).withLicenseDialog(true).start(this)
            true
        }
        val aboutMenu = binding.toolbar.menu.add(getString(R.string.about))
        aboutMenu.setOnMenuItemClickListener {
            val url = "https://www.armedpineapple.co.uk/projects/inno-setup-extractor"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
            true
        }
    }

    private fun updateMotion() {
        val binding = _binding

        if (binding?.bottomScrollView != null) {
            val scrollView = binding.bottomScrollView
            if (scrollView.canScrollVertically(-1) || scrollView.canScrollVertically(1)) {
                binding.mainMotion.enableTransition(R.id.drag_transition, true)
            } else {
                binding.mainMotion.enableTransition(R.id.drag_transition, false)
                binding.mainMotion.setConstraintSet(binding.mainMotion.getConstraintSet(R.id.expanded))
            }
        }
    }


    override fun onStart() {
        super.onStart()
        if (isServiceBound && extractService.isExtracting) {
            showProgressFragment()
        }

        if (shouldShowInstructions) {
            val introFragment = IntroFragment()
            introFragment.show(supportFragmentManager, "intro_fragment")
            shouldShowInstructions = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(connection)
        isServiceBound = false
    }

    companion object {
        private const val SELECTOR_FRAGMENT_TAG = "selector"
        private const val PROGRESS_FRAGMENT_TAG = "progress"
    }
}


