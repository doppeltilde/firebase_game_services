package tiltedcu.be.firebase_game_services_google

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.Gravity
import com.google.android.gms.auth.api.Auth
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
import androidx.annotation.NonNull

private const val CHANNEL_NAME = "firebase_game_services"
private const val RC_SIGN_IN = 9000

class FirebaseGameServicesGooglePlugin(private var activity: Activity? = null) : FlutterPlugin,
    MethodChannel.MethodCallHandler, ActivityAware, PluginRegistry.ActivityResultListener {

    private var achievementClient: AchievementsClient? = null
    private var leaderboardsClient: LeaderboardsClient? = null
    private var gamesClient: GamesClient? = null
    private var activityPluginBinding: ActivityPluginBinding? = null
    private var channel: MethodChannel? = null
    private var pendingOperation: PendingOperation? = null

    private lateinit var context: Context

    private var method: String? = null
    private var clientId: String? = null
    private var pendingResult: Result? = null
    private var forceSignInIfCredentialAlreadyUsed: Boolean = false

    companion object {
        @JvmStatic
        fun getResourceFromContext(@NonNull context: Context, resName: String): String {
            val stringRes = context.resources.getIdentifier(resName, "string", context.packageName)
            if (stringRes == 0) {
                throw IllegalArgumentException(String.format("The 'R.string.%s' value it's not defined in your project's resources file.", resName))
            }
            return context.getString(stringRes)
        }
    }

    private fun silentSignIn() {
        val activity = activity ?: return

        val gamesSignInClient = PlayGames.getGamesSignInClient(activity)

        // Doc Ref: https://developers.google.com/games/services/android/signin#remove_sign-in_and_sign-out_calls
        gamesSignInClient.isAuthenticated.addOnCompleteListener { isAuthenticatedTask ->
            val isAuthenticated = isAuthenticatedTask.isSuccessful &&
                    isAuthenticatedTask.result.isAuthenticated
            if (isAuthenticated) {
                handleSignInResult()
            } else {
                gamesSignInClient.signIn().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        handleSignInResult()
                    } else {
                        Log.e("Error: ", task.exception.toString())
                    }
                }
            }
        }
    }

    private fun handleSignInResult() {
        val activity = activity ?: return

        achievementClient = PlayGames.getAchievementsClient(activity)
        leaderboardsClient = PlayGames.getLeaderboardsClient(activity)

        gamesClient?.setViewForPopups(activity.findViewById(android.R.id.content))
        gamesClient?.setGravityForPopups(Gravity.TOP or Gravity.CENTER_HORIZONTAL)

        if (method == Methods.signIn) {
            signInFirebaseWithPlayGames()
        } else if (method == Methods.signInLinkedUser) {
            signInFirebaseWithPlayGames()
        }
    }

    private fun signInFirebaseWithPlayGames() {
        val auth = FirebaseAuth.getInstance()
        val activity = this.activity ?: return

        val authCode: String = clientId ?: getResourceFromContext(context, "default_web_client_id")

        // Doc Ref: https://developers.google.com/games/services/android/signin#request_server_side_access
        val gamesSignInClient = PlayGames.getGamesSignInClient(activity)
        gamesSignInClient.requestServerSideAccess(authCode, false).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val serverAuthToken = task.result

                val credential = PlayGamesAuthProvider.getCredential(serverAuthToken!!)

                auth.signInWithCredential(credential).addOnCompleteListener { task2 ->
                    pendingResult?.success(true)
                }
            } else {
                gamesSignInClient.requestServerSideAccess(authCode, false).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val serverAuthToken = task.result

                            val credential = PlayGamesAuthProvider.getCredential(serverAuthToken!!)
                            auth.currentUser?.linkWithCredential(credential)?.addOnCompleteListener { task2 ->
                                if (task2.isSuccessful) {
                                    pendingResult?.success(true)
                                } else {
                                    Log.e("Error:", task2.exception.toString())
                                    pendingResult?.success(false)
                                }
                            }
                        } else {
                            // Failed to retrieve authentication code.
                            Log.e("Error:", task.exception.toString())
                            pendingResult?.success(false)


                        }
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

    private fun showAllLeaderboards(result: Result) {
        showLoginErrorIfNotLoggedIn(result)
        leaderboardsClient?.allLeaderboardsIntent?.addOnSuccessListener { intent ->
            activity?.startActivityForResult(intent, 0)
            result.success("success")
        }?.addOnFailureListener {
            result.error("error", it.localizedMessage, null)
        }
    }

    private fun showSingleLeaderboard(leaderboardID: String, result: Result) {
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
        Log.i(pendingOperation?.method, "success")
        pendingOperation?.result?.success(true)
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
            Methods.showAllLeaderboards -> {
                showAllLeaderboards(result)
            }
            Methods.showSingleLeaderboard -> {
                val leaderboardID = call.argument<String>("leaderboardID") ?: ""
                showSingleLeaderboard(leaderboardID, result)
            }
            Methods.showAchievements -> {
                showAchievements(result)
            }
            Methods.signIn -> {
                method = Methods.signIn
                clientId = call.argument<String>("client_id")
                pendingResult = result
                silentSignIn()
            }
            Methods.signInLinkedUser -> {
                method = Methods.signInLinkedUser
                clientId = call.argument<String>("client_id")
                forceSignInIfCredentialAlreadyUsed =
                    call.argument<Boolean>("force_sign_in_credential_already_used") == true
                pendingResult = result
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
    const val showAllLeaderboards = "showAllLeaderboards"
    const val showSingleLeaderboard = "showSingleLeaderboard"
    const val showAchievements = "showAchievements"
    const val signIn = "signIn"
    const val signInLinkedUser =
        "signInLinkedUser"
    const val getPlayerID = "getPlayerID"
    const val getPlayerName = "getPlayerName"
}
