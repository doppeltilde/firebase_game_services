package keew.ee.firebase_game_services_google

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.ActivityCompat.startActivityForResult
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.games.*
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.PlayGamesAuthProvider
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry


private const val CHANNEL_NAME = "firebase_game_services"
private const val RC_SIGN_IN = 9000
private const val RC_ACHIEVEMENT_UI = 9003
private const val RC_LEADERBOARD_UI = 9004

class FirebaseGameServicesGooglePlugin(private var activity: Activity? = null) : FlutterPlugin,
    MethodChannel.MethodCallHandler, ActivityAware, PluginRegistry.ActivityResultListener {

    private var gamesSignInClient = PlayGames.getGamesSignInClient(activity!!)

    private var activityPluginBinding: ActivityPluginBinding? = null
    private var channel: MethodChannel? = null
    private var pendingOperation: PendingOperation? = null
    private lateinit var context: Context

    private var method: String? = null
    private var clientId: String? = null
    private var gResult: Result? = null
    private var forceSignInIfCredentialAlreadyUsed: Boolean = false

    private fun silentSignIn() {
        gamesSignInClient.isAuthenticated.addOnCompleteListener { isAuthTask: Task<AuthenticationResult> ->
            val isAuthenticated = isAuthTask.isSuccessful && isAuthTask.result.isAuthenticated
            if (isAuthenticated) {
                handleSignInResult()
            } else {
                Log.e("Error", "signInError", isAuthTask.exception)
                Log.i("ExplicitSignIn", "Trying explicit sign in")
                gamesSignInClient.signIn()
                    .addOnCompleteListener() { signInTask: Task<AuthenticationResult> ->
                        val isExplicitAuth =
                            signInTask.isSuccessful && signInTask.result.isAuthenticated
                        if (isExplicitAuth) {
                            handleSignInResult()
                        } else {
                            finishPendingOperationWithError(
                                signInTask.exception
                                    ?: Exception("ExplicitSignIn failed")
                            )
                        }
                    }
            }
        }
    }

    private fun handleSignInResult() {
        val activity = this.activity!!
        val account = GoogleSignIn.getLastSignedInAccount(activity)
        if (account != null) {
            if (method == "signIn") {
                signInFirebaseWithPlayGames(account)
            } else if (method == "signInLinkedUser") {
                linkCredentialsFirebaseWithPlayGames(account)
            }
        }
    }

    private fun signInFirebaseWithPlayGames(acct: GoogleSignInAccount) {
        val auth = FirebaseAuth.getInstance()
        val authCode = acct.serverAuthCode ?: throw Exception("auth_code_null")
        val credential = PlayGamesAuthProvider.getCredential(authCode)
        auth.signInWithCredential(credential).addOnCompleteListener { result ->
            if (result.isSuccessful) {
                finishPendingOperationWithSuccess()
            } else {
                finishPendingOperationWithError(
                    result.exception
                        ?: Exception("signInWithCredential failed")
                )
            }
        }
    }

    private fun linkCredentialsFirebaseWithPlayGames(acct: GoogleSignInAccount) {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser ?: throw Exception("current_user_null")
        val authCode = acct.serverAuthCode ?: throw Exception("auth_code_null")
        val credential = PlayGamesAuthProvider.getCredential(authCode)

        currentUser.linkWithCredential(credential).addOnCompleteListener { result ->
            if (result.isSuccessful) {
                finishPendingOperationWithSuccess()
            } else {
                if (result.exception is FirebaseAuthException) {
                    if ((result.exception as FirebaseAuthException).errorCode == "ERROR_CREDENTIAL_ALREADY_IN_USE" && forceSignInIfCredentialAlreadyUsed) {
                        method = "signIn"
                        silentSignIn()
                    } else {
                        finishPendingOperationWithError(
                            result.exception
                                ?: Exception("linkWithCredential failed")
                        )
                    }
                } else {
                    finishPendingOperationWithError(
                        result.exception
                            ?: Exception("linkWithCredential failed")
                    )
                }
            }
        }
    }


    //region Achievements & Leaderboards


    private fun showAchievements(result: Result) {
        showLoginErrorIfNotLoggedIn(result)
        val activity = this.activity!!
        PlayGames.getAchievementsClient(activity)
            .achievementsIntent
            .addOnSuccessListener { intent ->
                activity.startActivityForResult(intent, RC_ACHIEVEMENT_UI)
                result.success("success")
            }
        }


    private fun unlock(achievementID: String, result: Result) {
        showLoginErrorIfNotLoggedIn(result)
        val activity = this.activity!!
        PlayGames.getAchievementsClient(activity).unlockImmediate(achievementID)
            .addOnSuccessListener {
                result.success("success")
            }?.addOnFailureListener {
            result.error("error", it.localizedMessage, null)
        }
    }

    private fun increment(achievementID: String, count: Int, result: Result) {
        showLoginErrorIfNotLoggedIn(result)
        val activity = this.activity!!
        PlayGames.getAchievementsClient(activity).incrementImmediate(achievementID, count)
            .addOnSuccessListener {
                result.success("success")
            }?.addOnFailureListener {
                result.error("error", it.localizedMessage, null)
            }
    }

    private fun showLeaderboards(leaderboardID: String, result: Result) {
        showLoginErrorIfNotLoggedIn(result)
        val activity = this.activity!!

        val onSuccessListener: ((Intent) -> Unit) = { intent ->
            activity.startActivityForResult(intent, RC_LEADERBOARD_UI)
            result.success("success")
        }
        val onFailureListener: ((Exception) -> Unit) = {
            result.error("error", "${it.message}", null)
        }
        if (leaderboardID.isEmpty()) {
           PlayGames.getLeaderboardsClient(activity) .allLeaderboardsIntent.addOnSuccessListener(onSuccessListener)
                .addOnFailureListener(onFailureListener)
        } else {
            PlayGames.getLeaderboardsClient(activity).getLeaderboardIntent(leaderboardID)
                .addOnSuccessListener(onSuccessListener).addOnFailureListener(onFailureListener)
        }
    }

    private fun submitScore(leaderboardID: String, score: Int, result: Result) {
        showLoginErrorIfNotLoggedIn(result)
        val activity = this.activity!!

        PlayGames.getLeaderboardsClient(activity).submitScoreImmediate(leaderboardID, score.toLong())
            ?.addOnSuccessListener {
                result.success("success")
            }?.addOnFailureListener {
            result.error("error", it.localizedMessage, null)
        }
    }

    private fun showLoginErrorIfNotLoggedIn(result: Result) {
            result.error("error", "Please make sure to call signIn() first", null)
    }
    //endregion

    //region User
    private fun getPlayerID(result: Result) {
        showLoginErrorIfNotLoggedIn(result)
        val activity = activity ?: return
        PlayGames.getPlayersClient(activity)
            .currentPlayerId.addOnSuccessListener {
                result.success(it)
            }.addOnFailureListener {
                result.error("error", it.localizedMessage, null)
            }
    }

    private fun getPlayerName(result: Result) {
        showLoginErrorIfNotLoggedIn(result)
        val activity = activity ?: return
        PlayGames.getPlayersClient(activity)
            .currentPlayer
            .addOnSuccessListener { player ->
                result.success(player.displayName)
            }.addOnFailureListener {
                result.error("error", it.localizedMessage, null)
            }
    }
    //endregion

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        setupChannel(binding.binaryMessenger)
        context = binding.applicationContext
        PlayGamesSdk.initialize(context)
        val channel = MethodChannel(binding.binaryMessenger, CHANNEL_NAME)
        val plugin = FirebaseGameServicesGooglePlugin(activity)
        channel.setMethodCallHandler(plugin)
        activityPluginBinding?.addActivityResultListener(plugin)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        teardownChannel()
    }

    private fun setupChannel(messenger: BinaryMessenger) {
        channel = MethodChannel(messenger, CHANNEL_NAME)
        channel?.setMethodCallHandler(this)
    }

    private fun teardownChannel() {
        channel?.setMethodCallHandler(null)
        channel = null
    }

    private fun disposeActivity() {
        activityPluginBinding?.removeActivityResultListener(this)
        activityPluginBinding = null
    }

    override fun onDetachedFromActivity() {
        disposeActivity()
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activityPluginBinding = binding
        activity = binding.activity
        binding.addActivityResultListener(this)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity()
    }

    private class PendingOperation constructor(val method: String, val result: Result)

    private fun finishPendingOperationWithSuccess() {
        Log.i(pendingOperation!!.method, "success")
        pendingOperation!!.result.success(true)
        pendingOperation = null
    }

    private fun finishPendingOperationWithError(exception: Exception) {
        Log.i(pendingOperation!!.method, "error")

        when (exception) {
            is FirebaseAuthException -> {
                pendingOperation!!.result.error(
                    exception.errorCode,
                    exception.localizedMessage,
                    null
                )
            }
            is ApiException -> {
                pendingOperation!!.result.error(
                    exception.statusCode.toString(),
                    exception.localizedMessage,
                    null
                )
            }
            else -> {
                pendingOperation!!.result.error("error", exception.localizedMessage, null)
                pendingOperation = null
            }
        }
    }

    //region ActivityResultListener
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == RC_SIGN_IN) {
            val result = data?.let { Auth.GoogleSignInApi.getSignInResultFromIntent(it) }

            val signInAccount = result?.signInAccount

            if (result?.isSuccess == true && signInAccount != null) {
                handleSignInResult()
            } else {
                finishPendingOperationWithError(ApiException(result?.status ?: Status(0)))
                var message = result?.status?.statusMessage ?: ""
                if (message.isEmpty()) {
                    message = "Something went wrong " + result?.status
                }
            }
            return true
        }
        return false
    }
    //endregion


    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "unlock" -> {
                unlock(call.argument<String>("achievementID") ?: "", result)
            }
            "increment" -> {
                val achievementID = call.argument<String>("achievementID") ?: ""
                val steps = call.argument<Int>("steps") ?: 1
                increment(achievementID, steps, result)
            }
            "submitScore" -> {
                val leaderboardID = call.argument<String>("leaderboardID") ?: ""
                val score = call.argument<Int>("value") ?: 0
                submitScore(leaderboardID, score, result)
            }
            "showLeaderboards" -> {
                val leaderboardID = call.argument<String>("leaderboardID") ?: ""
                showLeaderboards(leaderboardID, result)
            }
            "showAchievements" -> {
                showAchievements(result)
            }
            "signIn" -> {
                method = "signIn"
                clientId = call.argument<String>("client_id")
                gResult = result
                silentSignIn()
            }
            "signInLinkedUser" -> {
                method = "signInLinkedUser"
                clientId = call.argument<String>("client_id")
                forceSignInIfCredentialAlreadyUsed =
                    call.argument<Boolean>("force_sign_in_credential_already_used") == true
                gResult = result
                silentSignIn()
            }
            "getPlayerID" -> {
                getPlayerID(result)
            }
            "getPlayerName" -> {
                getPlayerName(result)
            }
            else -> {
                result.notImplemented()
            }
        }
    }
}