package com.leminect.stranzee.view

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.*
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.tasks.Task
import com.google.gson.Gson
import com.leminect.stranzee.R
import com.leminect.stranzee.adapter.bindImageUrl
import com.leminect.stranzee.databinding.ActivityMainBinding
import com.leminect.stranzee.network.RoomData
import com.leminect.stranzee.network.SocketManager
import com.leminect.stranzee.utility.openPlayStore
import com.leminect.stranzee.viewmodel.LOGOUT
import com.leminect.stranzee.viewmodel.MainViewModel
import com.leminect.stranzee.viewmodelfactory.MainViewModelFactory
import de.hdodenhof.circleimageview.CircleImageView
import io.socket.emitter.Emitter


class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    lateinit var drawerLayout: DrawerLayout
    lateinit var navController: NavController
    lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var viewModel: MainViewModel
    private lateinit var token: String
    private lateinit var refreshToken: String
    private lateinit var userId: String
    private lateinit var prefs: SharedPreferences
    private lateinit var headerTextView: TextView
    private lateinit var headerImageView: CircleImageView
    private lateinit var errorHybridDialog: HybridDialog
    private lateinit var failedHybridDialog: HybridDialog
    private lateinit var loadingDialog: CustomLoadingDialog

    companion object {
        const val UPDATE_REQUEST_CODE = 102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)

        prefs = getSharedPreferences(getString(R.string.shared_prefs_name), MODE_PRIVATE)
        userId = prefs.getString(getString(R.string.prefs_user_id), "")!!
        token = prefs.getString(getString(R.string.prefs_token), "")!!
        refreshToken = prefs.getString(getString(R.string.prefs_refresh_token), "")!!

        val viewModelFactory = MainViewModelFactory(token, refreshToken, userId)
        viewModel = ViewModelProvider(this, viewModelFactory).get(MainViewModel::class.java)

        loadingDialog = CustomLoadingDialog(this, false, "Logging out...")

        errorHybridDialog = HybridDialog(
            this,
            arrayOf(View.VISIBLE, View.GONE, View.GONE, View.VISIBLE, View.GONE),
            arrayOf("Log Out Error",
                "",
                "",
                "An error occurred during the log out process. Please check your internet connection!"),
            true,
            object : OkButtonListener {
                override fun onOkClick(interests: String, dismissDialog: (Boolean) -> Unit) {
                    dismissDialog(true)
                }
            }
        )
        failedHybridDialog = HybridDialog(
            this,
            arrayOf(View.VISIBLE, View.GONE, View.GONE, View.VISIBLE, View.GONE),
            arrayOf("Log out Failed",
                "",
                "",
                "An error occurred during the log out process. Please try again!"),
            true,
            object : OkButtonListener {
                override fun onOkClick(interests: String, dismissDialog: (Boolean) -> Unit) {
                    dismissDialog(true)
                }
            }
        )

        viewModel.tokenCheckData.observe(this, Observer { data ->
            data?.let {
                viewModel.onTokenDataChecked()

                if (data.authorized) {
                    if (data.token != token) {
                        prefs.edit().putString(getString(R.string.prefs_token), data.token).commit()
                        prefs.edit().putString(getString(R.string.prefs_refresh_token), data.refreshToken).commit()

                        if(data.restartOnTokenChange) {
                            Toast.makeText(this,
                                "Session expired. Restarting app...",
                                Toast.LENGTH_SHORT).show()

                            // restart the app
                            val intent = Intent(this, SplashScreen::class.java)
                            finishAffinity()
                            this.startActivity(intent)
                        }
                    }
                } else {
                    Toast.makeText(this,
                        "Session expired. Please login to continue.",
                        Toast.LENGTH_LONG).show()
                    goToLoginActivity()
                }
            }
        })

        viewModel.logout.observe(this, Observer { status ->
            status.let {
                when(status) {
                    LOGOUT.LOGGING_OUT -> loadingDialog.showDialog()
                    LOGOUT.LOGOUT_ERROR -> {
                        loadingDialog.dismissDialog()
                        errorHybridDialog.showDialog()
                        viewModel.onLogoutComplete()
                    }
                    LOGOUT.LOGOUT_FAILED -> {
                        loadingDialog.dismissDialog()
                        failedHybridDialog.showDialog()
                        viewModel.onLogoutComplete()
                    }
                    LOGOUT.LOGOUT_SUCCESSFUL -> {
                        loadingDialog.dismissDialog()
                        viewModel.onLogoutComplete()
                        goToLoginActivity()
                    }
                }
            }
        })

        SocketManager.setToken(token)
        SocketManager.setUserId(userId)
        /*val serviceIntent = Intent(baseContext, NotificationService::class.java)
        serviceIntent.putExtra("userId", userId)
        serviceIntent.putExtra("token", token)
        startService(serviceIntent)*/

        drawerLayout = binding.drawerLayout
        val navHeaderView = binding.navView.getHeaderView(0)
        headerTextView = navHeaderView.findViewById<TextView>(R.id.drawer_header_text)
        headerImageView = navHeaderView.findViewById<CircleImageView>(R.id.drawer_header_image)

        navController = this.findNavController(R.id.navHostFragment)
        binding.bottomNavigationView.setupWithNavController(navController)
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.findFragment, R.id.chatFragment, R.id.savedFragment, R.id.profileFragment),
            binding.drawerLayout)


        if(intent.getStringExtra("notificationType") == "chat") {
            val navHostFragment =
                (supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment)
            val inflater = navHostFragment.navController.navInflater
            val graph = inflater.inflate(R.navigation.main_navigation)
            graph.startDestination = R.id.chatFragment
            navHostFragment.navController.graph = graph
        }

        setSupportActionBar(binding.topAppBar)
        setUpCustomActionBar()

        binding.navView.setNavigationItemSelectedListener { menuItem ->
            // Handle menu item selected
            when (menuItem.itemId) {
                R.id.who_checked_me_menu -> {
                    val intent: Intent = Intent(this@MainActivity, WhoCheckedMeActivity::class.java)
                    startActivity(intent)
                }
                R.id.log_out -> {
                    HybridDialog(this,
                        arrayOf(View.VISIBLE, View.GONE, View.GONE, View.VISIBLE, View.VISIBLE),
                        arrayOf(getString(R.string.log_out),
                            "",
                            "",
                            "Are you sure you want to log out?"),
                        false,
                        object : OkButtonListener {
                            override fun onOkClick(
                                interests: String,
                                dismissDialog: (Boolean) -> Unit,
                            ) {
                                dismissDialog(true)
                                // goToLoginActivity()
                                viewModel.startLogoutProcess(token, userId)
                            }
                        }, "Yes").showDialog()
                }
                R.id.rate_us -> {
                    RateUsDialog(this, true)
                }
                R.id.contact_us -> {
                    HybridDialog(
                        this,
                        arrayOf(View.VISIBLE, View.GONE, View.GONE, View.VISIBLE, View.GONE),
                        arrayOf("Contact us",
                            "",
                            "",
                            getString(R.string.contact_us_text, getString(R.string.admin_email))),
                        true,
                        object : OkButtonListener {
                            override fun onOkClick(
                                interests: String,
                                dismissDialog: (Boolean) -> Unit,
                            ) {
                                dismissDialog(true)
                            }
                        }
                    ).showDialog()
                }
                R.id.share -> {
                    val shareIntent: Intent = Intent(Intent.ACTION_SEND)
                    shareIntent.setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT,
                            getString(R.string.share_text,
                                getString(R.string.app_name),
                                packageName))

                    if (shareIntent.resolveActivity(this.packageManager) != null) {
                        startActivity(shareIntent)
                    } else {
                        Toast.makeText(this,
                            "No suitable apps found for sharing the information. You can share our app manually :)",
                            Toast.LENGTH_LONG).show()
                    }
                }
            }

            menuItem.isChecked = true
            drawerLayout.close()
            true
        }

        refreshDrawerHeader()
        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
            }

            override fun onDrawerOpened(drawerView: View) {
                refreshDrawerHeader()
            }

            override fun onDrawerClosed(drawerView: View) {
            }

            override fun onDrawerStateChanged(newState: Int) {
            }
        })

        val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(this)
        val appUpdateInfoTask: Task<AppUpdateInfo> = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { result: AppUpdateInfo ->
            if(result.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && result.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                try {
                    appUpdateManager.startUpdateFlowForResult(result, AppUpdateType.FLEXIBLE, this, UPDATE_REQUEST_CODE)
                } catch (ignored: Exception) { }
            }
        }

        val appOpens: Int = prefs.getInt(getString(R.string.prefs_open_count), 0)
        val ratingGiven: Boolean = prefs.getBoolean(getString(R.string.prefs_rating_given), false)
        // open rate dialog after first 3 app opens and then after each 4 app opens if rating not given
        if(!ratingGiven && appOpens % 4 == 2) {
            RateUsDialog(this)
        }

        prefs.edit().putInt(getString(R.string.prefs_open_count), appOpens + 1).apply()
    }

    /*var onConnect = Emitter.Listener {
        //After getting a Socket.EVENT_CONNECT which indicate socket has been connected to server,
        //send online status to the server.
        mSocket.emit("status", "online")
    }*/

    var onDataFromServer = Emitter.Listener {
        Log.i("MainActivitySocket", it.toString())
    }


    private fun refreshDrawerHeader() {
        val fullName = prefs.getString(getString(R.string.prefs_firstName), "") + " " +
                prefs.getString(getString(R.string.prefs_lastName), "")
        headerTextView.text = fullName

        bindImageUrl(headerImageView,
            prefs.getString(getString(R.string.prefs_image_url), null),
            prefs.getString(getString(R.string.prefs_user_id), null))
    }

    /*override fun onBackPressed() {
        finish()
    }*/

    private fun goToLoginActivity() {
        /*SocketManager.getSocket()?.emit("unsubscribe", Gson().toJson(
            RoomData(userId, "", "notification", token)
        ))
        stopService(Intent(baseContext, NotificationService::class.java))*/

        prefs.edit().clear().commit()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun setUpCustomActionBar() {
        val actionbar = supportActionBar
        actionbar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        actionbar?.setCustomView(R.layout.main_action_bar)

        actionbar?.customView?.findViewById<ImageView>(R.id.slider_menu_button)
            ?.setOnClickListener {
                onSupportNavigateUp()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration)
    }

    override fun onResume() {
        super.onResume()
        SocketManager.setOnline(true)
    }

    override fun onPause() {
        super.onPause()
        SocketManager.setOnline(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        // comment the line below(having disconnect()) if NotificationService is running
        // currently Firebase is used for notification
        SocketManager.getSocket()?.disconnect()
    }
}