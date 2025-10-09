package com.example.myapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.myapp.presentation.ViewModelFactory
import com.example.myapp.presentation.contacts.ContactsViewModel
import com.example.myapp.presentation.main.MainUiState
import com.example.myapp.presentation.main.MainViewModel
import com.example.myapp.utils.FeaturePermissionHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var contactsViewModel: ContactsViewModel
    private lateinit var mainViewModel: MainViewModel
    private lateinit var viewModelFactory: ViewModelFactory
    private var isSirenActive = false

    private var mediaPlayer: MediaPlayer? = null
    private var colorAnimator: ValueAnimator? = null
    private val sirenHandler = Handler(Looper.getMainLooper())

    // Colors for the siren animation
    private val sirenColors = listOf(
        Color.RED,
        Color.rgb(255, 100, 0),
        Color.YELLOW,
        Color.rgb(255, 0, 255),
        Color.RED,
        Color.rgb(255, 50, 50),
        Color.rgb(255, 150, 0)
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModels with Clean Architecture
        val app = requireActivity().application as MyApplication
        viewModelFactory = ViewModelFactory(app.appModule)
        contactsViewModel = ViewModelProvider(requireActivity(), viewModelFactory)[ContactsViewModel::class.java]
        mainViewModel = ViewModelProvider(requireActivity(), viewModelFactory)[MainViewModel::class.java]

        // Set up contact status views (non-clickable)
//        view.findViewById<android.widget.TextView>(R.id.tv_contacts_configured)
//        view.findViewById<android.widget.TextView>(R.id.tv_contacts_not_configured)

        // Set up contacts observer
//        setupContactsObserver(view)

        // Set up emergency button click listener with Clean Architecture
        view.findViewById<Button>(R.id.btn_activate_emergency).setOnClickListener {
            mainViewModel.handleEvent(MainViewModel.UiEvent.SosClicked)
        }

        // Observe MainViewModel state
        observeMainViewModel()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // Hidden Camera Detector Click Handler
        view.findViewById<CardView>(R.id.card_camera_detector).setOnClickListener {
            CameraDetectorActivity.start(requireActivity() as androidx.appcompat.app.AppCompatActivity)
        }

        // Emergency Helpline Click Handler
        view.findViewById<CardView>(R.id.card_emergency_helpline).setOnClickListener {
            startActivity(Intent(requireContext(), EmergencyHelplineActivity::class.java))
        }

        // Siren Click Handler
        view.findViewById<CardView>(R.id.card_siren).setOnClickListener {
            if (isSirenActive) {
                stopSiren()
            } else {
                showSirenConfirmationDialog()
            }
        }

        // Contacts Card Click Handler - Navigates to SosActivity (Emergency Contacts)
        view.findViewById<CardView>(R.id.card_contacts).setOnClickListener {
            startActivity(Intent(requireContext(), SosActivity::class.java))
        }

        // Nearby search handlers
        fun openNearbySearch(query: String) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                FeaturePermissionHelper.Companion.LocationFeature.checkPermissions(requireActivity()) {
                    performNearbySearch(query)
                }
                return
            }
            performNearbySearch(query)
        }

        view.findViewById<CardView>(R.id.card_police).setOnClickListener {
            openNearbySearch("nearby police station")
        }
        view.findViewById<CardView>(R.id.card_hospital).setOnClickListener {
            openNearbySearch("nearby hospital")
        }
        view.findViewById<CardView>(R.id.card_pharmacy).setOnClickListener {
            openNearbySearch("nearby pharmacy")
        }
        view.findViewById<CardView>(R.id.card_women_shelter).setOnClickListener {
            openNearbySearch("nearby women's shelter")
        }

        // Explore card binding (after layout)
        val rootView = view.findViewById<androidx.coordinatorlayout.widget.CoordinatorLayout>(R.id.main)
        val viewTreeObserver = rootView.viewTreeObserver
        viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
            private var isCardFound = false
            override fun onGlobalLayout() {
                if (isCardFound) {
                    rootView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    return
                }
                fun findExploreCard(v: View): View? {
                    if (v is ViewGroup) {
                        if (v is FrameLayout && v.childCount > 0) {
                            for (i in 0 until v.childCount) {
                                val child = v.getChildAt(i)
                                if (child is android.widget.TextView && child.text.toString().contains("Explore")) {
                                    return v
                                }
                            }
                        }
                        for (i in 0 until v.childCount) {
                            val found = findExploreCard(v.getChildAt(i))
                            if (found != null) return found
                        }
                    }
                    return null
                }
                val exploreCard = findExploreCard(rootView)
                if (exploreCard != null) {
                    isCardFound = true
                    exploreCard.setOnClickListener {
                        startActivity(Intent(requireContext(), ExploreLifesaveActivity::class.java))
                    }
                    rootView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }
        })

        // Load siren state
        checkSirenState()
    }

    override fun onResume() {
        super.onResume()
        // Refresh contacts when returning to the fragment
        contactsViewModel.handleEvent(ContactsViewModel.UiEvent.LoadContacts)
        // Check siren state
        checkSirenState()
    }

//    private fun setupContactsObserver(view: View) {
//        viewLifecycleOwner.lifecycleScope.launch {
//            contactsViewModel.uiState.collect { state ->
//                val hasContacts = state.contacts.isNotEmpty()
////                val tvContactsConfigured = view.findViewById<android.widget.TextView>(R.id.tv_contacts_configured)
////                val tvContactsNotConfigured = view.findViewById<android.widget.TextView>(R.id.tv_contacts_not_configured)
//                if (hasContacts) {
//                    tvContactsConfigured.visibility = View.VISIBLE
//                    tvContactsNotConfigured.visibility = View.GONE
//                } else {
//                    tvContactsConfigured.visibility = View.GONE
//                    tvContactsNotConfigured.visibility = View.VISIBLE
//                }
//            }
//        }
//        contactsViewModel.handleEvent(ContactsViewModel.UiEvent.LoadContacts)
//    }

    private fun observeMainViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.uiState.collect { state ->
                handleMainUiState(state)
            }
        }
        mainViewModel.handleEvent(MainViewModel.UiEvent.CheckPermissions)
    }

    private fun handleMainUiState(state: MainUiState) {
        if (state.isLoading) {
            // optional loading indicator
        }
        state.error?.let { error ->
            if (error.contains("SMS permission", ignoreCase = true)) {
                FeaturePermissionHelper.Companion.SOSFeature.checkPermissions(requireActivity()) {}
                mainViewModel.handleEvent(MainViewModel.UiEvent.DismissError)
            } else {
                AlertDialog.Builder(requireContext())
                    .setTitle("Error")
                    .setMessage(error)
                    .setPositiveButton("OK") { _, _ ->
                        mainViewModel.handleEvent(MainViewModel.UiEvent.DismissError)
                    }
                    .show()
            }
        }
        state.successMessage?.let { message ->
            AlertDialog.Builder(requireContext())
                .setTitle("Success")
                .setMessage(message)
                .setPositiveButton("OK") { _, _ ->
                    mainViewModel.handleEvent(MainViewModel.UiEvent.DismissSuccess)
                }
                .show()
        }
        state.nearbySearchQuery?.let { query ->
            performNearbySearch(query)
        }
    }

    private fun checkSirenState() {
        val sharedPref = requireContext().getSharedPreferences("SirenState", Context.MODE_PRIVATE)
        isSirenActive = sharedPref.getBoolean("isSirenActive", false)
        updateSirenCardUI()
    }

    private fun saveSirenState(isActive: Boolean) {
        val sharedPref = requireContext().getSharedPreferences("SirenState", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("isSirenActive", isActive)
            apply()
        }
    }

    private fun showSirenConfirmationDialog() {
        var dialog: AlertDialog? = null
        var isCanceled = false
        dialog = AlertDialog.Builder(requireContext())
            .setTitle("Siren Activation")
            .setMessage("Siren will start in 3 seconds")
            .setPositiveButton("Undo") { _, _ -> isCanceled = true }
            .setCancelable(false)
            .create()
        dialog.show()
        sirenHandler.postDelayed({
            if (dialog?.isShowing == true) {
                dialog.dismiss()
                if (!isCanceled) {
                    isSirenActive = true
                    saveSirenState(true)
                    updateSirenCardUI()
                    startSiren()
                }
            }
        }, 3000)
    }

    private fun startSiren() {
        try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(requireContext(), R.raw.siren2).apply {
                    isLooping = true
                    start()
                }
            }
            startSirenCardAnimation()
            Toast.makeText(requireContext(), "Siren started", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("HomeFragment", "Failed to start siren", e)
            Toast.makeText(requireContext(), "Failed to start siren", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startSirenCardAnimation() {
        val sirenCard = view?.findViewById<CardView>(R.id.card_siren) ?: return
        colorAnimator?.cancel()
        colorAnimator = ValueAnimator().apply {
            setObjectValues(*sirenColors.toTypedArray())
            setEvaluator(ArgbEvaluator())
            duration = 800
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                if (isSirenActive) {
                    val color = animator.animatedValue as Int
                    sirenCard.setCardBackgroundColor(color)
                }
            }
            start()
        }
    }

    private fun stopSiren(silent: Boolean = false) {
        isSirenActive = false
        saveSirenState(false)
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
                it.release()
                mediaPlayer = null
            }
        }
        colorAnimator?.cancel()
        updateSirenCardUI()
        if (!silent) {
            Toast.makeText(requireContext(), "Siren stopped", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateSirenCardUI() {
        val sirenCard = view?.findViewById<CardView>(R.id.card_siren) ?: return
        if (isSirenActive) {
            sirenCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.emergency_red_dark))
            val sirenText = sirenCard.findViewById<android.widget.TextView>(R.id.siren_title)
            val sirenDesc = sirenCard.findViewById<android.widget.TextView>(R.id.siren_description)
            sirenText?.text = "Stop Siren"
            sirenDesc?.text = "Tap to stop alarm"
        } else {
            sirenCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface_secondary))
            val sirenText = sirenCard.findViewById<android.widget.TextView>(R.id.siren_title)
            val sirenDesc = sirenCard.findViewById<android.widget.TextView>(R.id.siren_description)
            sirenText?.text = "Siren"
            sirenDesc?.text = "Play emergency alarm"
        }
    }

    private fun performNearbySearch(query: String) {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            openGeneralSearch(query)
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                try {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    val gmmIntentUri = Uri.parse("geo:$latitude,$longitude?q=$query&z=14")
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    mapIntent.setPackage("com.google.android.apps.maps")
                    if (mapIntent.resolveActivity(requireContext().packageManager) != null) {
                        startActivity(mapIntent)
                    } else {
                        val browserIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://www.google.com/maps/search/$query/@$latitude,$longitude,14z")
                        )
                        startActivity(browserIntent)
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                openGeneralSearch(query)
            }
        }.addOnFailureListener {
            openGeneralSearch(query)
        }
    }

    private fun openGeneralSearch(query: String) {
        try {
            val gmmIntentUri = Uri.parse("geo:0,0?q=nearby $query")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            if (mapIntent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(mapIntent)
            } else {
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/search/nearby+$query")
                )
                startActivity(browserIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Do not force-stop siren here to preserve behavior across tabs
        sirenHandler.removeCallbacksAndMessages(null)
        colorAnimator?.apply {
            removeAllUpdateListeners()
            removeAllListeners()
            cancel()
        }
        colorAnimator = null
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release media player if it's still active when the fragment is destroyed
        mediaPlayer?.apply {
            try {
                if (isPlaying) stop()
            } catch (_: Exception) {}
            release()
        }
        mediaPlayer = null
    }
}
