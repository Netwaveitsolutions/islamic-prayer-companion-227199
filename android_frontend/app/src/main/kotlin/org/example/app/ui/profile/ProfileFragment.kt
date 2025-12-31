package org.example.app.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import org.example.app.R
import org.example.app.data.prefs.AppPreferences
import org.example.app.domain.UserMode

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var prefs: AppPreferences

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        runCatching {
            val account = task.result
            if (account != null) {
                prefs.setUserMode(UserMode.GOOGLE)
                prefs.setGoogleAccountId(account.id)
                render()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        prefs = AppPreferences(requireContext())

        view.findViewById<MaterialButton>(R.id.btnGoogle).setOnClickListener { signInWithGoogle() }
        view.findViewById<MaterialButton>(R.id.btnGuest).setOnClickListener {
            prefs.setUserMode(UserMode.GUEST)
            prefs.setGoogleAccountId(null)
            render()
        }
        view.findViewById<MaterialButton>(R.id.btnSignOut).setOnClickListener {
            signOutGoogle()
        }

        render()
    }

    private fun render() {
        val tvStatus = requireView().findViewById<TextView>(R.id.tvProfileStatus)
        val tvDetails = requireView().findViewById<TextView>(R.id.tvProfileDetails)
        val btnSignOut = requireView().findViewById<MaterialButton>(R.id.btnSignOut)

        when (prefs.getUserMode()) {
            UserMode.GUEST -> {
                tvStatus.text = "Guest mode"
                tvDetails.text = "Your data is stored locally on this device."
                btnSignOut.visibility = View.GONE
            }
            UserMode.GOOGLE -> {
                val acct = GoogleSignIn.getLastSignedInAccount(requireContext())
                tvStatus.text = "Google signed-in"
                tvDetails.text = listOfNotNull(
                    acct?.displayName?.let { "Name: $it" },
                    acct?.email?.let { "Email: $it" },
                    acct?.id?.let { "ID: $it" }
                ).joinToString("\n")
                btnSignOut.visibility = View.VISIBLE
            }
        }
    }

    private fun signInWithGoogle() {
        // Note: For full Google Sign-In flow on a real device, the project must be configured
        // with appropriate OAuth client in Google Cloud + (commonly) google-services.json.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(requireContext(), gso)
        launcher.launch(client.signInIntent)
    }

    private fun signOutGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(requireContext(), gso)
        client.signOut().addOnCompleteListener {
            prefs.setUserMode(UserMode.GUEST)
            prefs.setGoogleAccountId(null)
            render()
        }
    }
}
