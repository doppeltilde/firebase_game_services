package tiltedcu.be.firebase_game_services_google

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.Gravity
import com.google.android.gms.auth.api.Auth
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

class FirebaseGameServicesGooglePlugin(private var activity: Activity? = null) : FlutterPlugin,
    MethodChannel.MethodCallHandler, ActivityAware, PluginRegistry.ActivityResultListener {

    private var achievementClient: AchievementsClient? = null
    private var leaderboardsClient: LeaderboardsClient? = null
    private var activityPluginBinding: ActivityPluginBinding? = null
    private var channel: MethodChannel? = null
    private var pendingOperation: PendingOperation? = null

    private lateinit var context: Context

    private var method: String? = null
    private var clientId: String? = null
    private var gResult: Result? = null
    private var forceSignInIfCredentialAlreadyUsed: Boolean = false

    companion object {
        @JvmStatic
        fun getResourceFromContext(context: Context, resName: String): String {
            val stringRes = context.resources.getIdentifier(resName, "string", context.packageName)
            if (stringRes == 0) {
                throw IllegalArgumentException(
                    String.format(
                        "The 'R.string.%s' value it's not defined in your project's resources file.",
                        resName
                    )
                )
            }
            return context.getString(stringRes)
        }
    }

    private fun silentSignIn() {
        val activity = activity ?: return

        val gamesSignInClient = PlayGames.getGamesSignInClient(activity)

        gamesSignInClient.isAuthenticated.addOnCompleteListener { isAuthenticatedTask ->
            val isAuthenticated = isAuthenticatedTask.isSuccessful &&
                    isAuthenticatedTask.result.isAuthenticated
            if (isAuthenticated) {
                Log.i("AUTH: ", isAuthenticatedTask.result.toString())

                handleSignInResult()


            } else {
                // Disable your integration with Play Games Services or show a
                // login button to ask  players to sign-in. Clicking it should
                // call GamesSignInClient.signIn().
                gamesSignInClient.signIn().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                      
                    } else {
                        // Player will need to sign-in explicitly using via UI
                        // prompt
                    }
                }
            }
        }
    }

    private fun handleSignInResult() {
        val activity = this.activity!!

        achievementClient = PlayGames.getAchievementsClient(activity)
        leaderboardsClient = PlayGames.getLeaderboardsClient(activity)

            if (method == Methods.signIn) {
                signInFirebaseWithPlayGames()
            } else if (method == Methods.signInLinkedUser) {

            }
    }

    private fun signInFirebaseWithPlayGames() {
        val auth = FirebaseAuth.getInstance()
        val activity = this.activity!!

        val authCode = clientId ?: getResourceFromContext(context, "default_web_client_id")

        val gamesSignInClient = PlayGames.getGamesSignInClient(activity)

        gamesSignInClient.requestServerSideAccess(authCode, false).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val serverAuthToken = task.result
                Log.d("serverAuthToken", serverAuthToken)
                val credential = PlayGamesAuthProvider.getCredential(serverAuthToken!!)

                auth.signInWithCredential(credential).addOnCompleteListener { task2 ->
                    Log.d("Success", task2.result.toString())
                    val user = auth.currentUser
                    Log.d("user: ", user.toString())

                    // Fix: Crash! java.lang.NullPointerException
                    pendingOperation!!.result.success(true)
                    pendingOperation = null;

                }
            } else {
                gamesSignInClient
                    .requestServerSideAccess(
                        authCode,  /*forceRefreshToken=*/
                        false
                    )
                    .addOnCompleteListener { task: Task<String?> ->
                        if (task.isSuccessful) {
                            Log.i("Result1: ", task.result.toString())

                            val serverAuthToken = task.result

                            val credential = PlayGamesAuthProvider.getCredential(serverAuthToken!!)
                            auth.currentUser?.linkWithCredential(credential)?.addOnCompleteListener { result ->
                                Log.i("isSuccess", result.isSuccessful.toString())
                                Log.i("serverAuthToken: ", serverAuthToken.toString())
                                Log.i("credential: ", credential.toString())
                                Log.i("Result2: ", result.toString())
                                if (result.isSuccessful) {
                                    Log.i("serverAuthToken: ", serverAuthToken.toString())
                                    Log.i("credential: ", credential.toString())
                                    Log.i("Result2: ", result.toString())
                                    //  finishPendingOperationWithSuccess()
                                } else {

                                }
                            }


                            // Send authentication code to the backend game server to be
                            // exchanged for an access token and used to verify the
                            // player via the Play Games Services REST APIs.
                        } else {
                            // Failed to retrieve authentication code.
                        }
                    }
            }
            }
    }

    private fun linkCredentialsFirebaseWithPlayGames(acct: GoogleSignInAccount) {
        val auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser ?: throw  Exception("current_user_null")

        val authCode = acct.serverAuthCode ?: throw Exception("auth_code_null")

        val credential = PlayGamesAuthProvider.getCredential(authCode)

        currentUser.linkWithCredential(credential).addOnCompleteListener { result ->
            if (result.isSuccessful) {
                finishPendingOperationWithSuccess()
            } else {
                if (result.exception is FirebaseAuthException) {
                    if ((result.exception as FirebaseAuthException).errorCode == "ERROR_CREDENTIAL_ALREADY_IN_USE" && forceSignInIfCredentialAlreadyUsed) {
                        method = Methods.signIn
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
    //endregion

    //region Achievements & Leaderboards
    private fun showAchievements(result: Result) {
        showLoginErrorIfNotLoggedIn(result)
        achievementClient?.achievementsIntent?.addOnSuccessListener { intent ->
            activity?.startActivityForResult(intent, 0)
            result.success("success")
        }?.addOnFailureListener {
            result.error("error", "${it.message}", null)
        }
    }

    private fun unlock(achievementID: String, result: Result) {
        showLoginErrorIfNotLoggedIn(result)
            achievementClient?.unlockImmediate(achievementID)?.addOnSuccessListener {
            result.success("success")
        }?.addOnFailureListener {
            result.error("error", it.localizedMessage, null)
        }
    }

    private fun increment(achievementID: String, count: Int, result: Result) {
        showLoginErrorIfNotLoggedIn(result)
        achievementClient?.incrementImmediate(achievementID, count)
            ?.addOnSuccessListener {
            result.success("success")
        }?.addOnFailureListener {
            result.error("error", it.localizedMessage, null)
        }
    }

    private fun showLeaderboards(leaderboardID: String, result: Result) {
        showLoginErrorIfNotLoggedIn(result)
        leaderboardsClient?.getLeaderboardIntent(leaderboardID)?.addOnSuccessListener { intent ->
            activity?.startActivityForResult(intent, 0)
            result.success("success")
        }?.addOnFailureListener {
            result.error("error", it.localizedMessage, null)
        }
    }

    private fun submitScore(leaderboardID: String, score: Int, result: Result) {
        showLoginErrorIfNotLoggedIn(result)
        leaderboardsClient?.submitScoreImmediate(leaderboardID, score.toLong())?.addOnSuccessListener {
        result.success("success")
        }?.addOnFailureListener {
        result.error("error", it.localizedMessage, null)
        }
    }

    private fun showLoginErrorIfNotLoggedIn(result: Result) {
        if (achievementClient == null || leaderboardsClient == null) {
        result.error("error", "Please make sure to call signIn() first", null)
        }
    }
    //endregion

    //region User
    private fun getPlayerID(result: Result) {
        showLoginErrorIfNotLoggedIn(result)
        val activity = activity ?: return

        PlayGames.getPlayersClient(activity)
            .currentPlayer.addOnCompleteListener { task: Task<Player?> ->
                result.success(task.result?.playerId)
            }.addOnFailureListener { exception: Exception ->
                result.error("error", exception.localizedMessage, null)
            }
    }

    private fun getPlayerName(result: Result) {
        showLoginErrorIfNotLoggedIn(result)
        val activity = activity ?: return

        PlayGames.getPlayersClient(activity)
            .currentPlayer
            .addOnCompleteListener { task: Task<Player?> ->
                result.success(task.result?.displayName)
            }.addOnFailureListener { exception: Exception ->
                result.error("error", exception.localizedMessage, null)
            }
    }
    //endregion

    //region Events

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        setupChannel(binding.binaryMessenger)
        context = binding.applicationContext
        PlayGamesSdk.initialize(context)
        Log.i("LOG: ", "onAttachedToEngine")
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
        if (pendingOperation != null) {
            Log.d(pendingOperation!!.method, "success")
            pendingOperation!!.result.success(true)
            pendingOperation = null
        } else {
            Log.d("pendingOperation", pendingOperation.toString())
        }
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
                pendingOperation!!.result.error(
                    "error",
                    exception.localizedMessage,
                    null
                )
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
            Methods.unlock -> {
                unlock(call.argument<String>("achievementID") ?: "", result)
            }
            Methods.increment -> {
                val achievementID = call.argument<String>("achievementID") ?: ""
                val steps = call.argument<Int>("steps") ?: 1
                increment(achievementID, steps, result)
            }
            Methods.submitScore -> {
                val leaderboardID = call.argument<String>("leaderboardID") ?: ""
                val score = call.argument<Int>("value") ?: 0
                submitScore(leaderboardID, score, result)
            }
            Methods.showLeaderboards -> {
                val leaderboardID = call.argument<String>("leaderboardID") ?: ""
                showLeaderboards(leaderboardID, result)
            }
            Methods.showAchievements -> {
                showAchievements(result)
            }
            Methods.signIn -> {
                method = Methods.signIn
                clientId = call.argument<String>("client_id")
                gResult = result
                silentSignIn()
            }
            Methods.signInLinkedUser -> {
                method = Methods.signInLinkedUser
                clientId = call.argument<String>("client_id")
                forceSignInIfCredentialAlreadyUsed =
                    call.argument<Boolean>("force_sign_in_credential_already_used") == true
                gResult = result
                silentSignIn()
            }
            Methods.getPlayerID -> {
                getPlayerID(result)
            }
            Methods.getPlayerName -> {
                getPlayerName(result)
            }
            else -> {
                result.notImplemented()
            }
        }
    }
}

object Methods {
    const val unlock = "unlock"
    const val increment = "increment"
    const val submitScore = "submitScore"
    const val showLeaderboards = "showLeaderboards"
    const val showAchievements = "showAchievements"
    const val signIn = "signIn"
    const val signInLinkedUser =
        "signInLinkedUser"
    const val getPlayerID = "getPlayerID"
    const val getPlayerName = "getPlayerName"
}
