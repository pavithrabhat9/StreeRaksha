package com.example.myapp

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView

class TipsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tips, container, false)

        setupToolbar(view)
        setupCardClicks(view)

        return view
    }

    private fun setupToolbar(view: View) {
        val toolbar = view.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    private fun setupCardClicks(view: View) {
        // Personal Safety Card
        view.findViewById<MaterialCardView>(R.id.card_personal_safety).setOnClickListener {
            showDetailBottomSheet(
                "Personal Safety",
                "Trust Your Instincts",
                """
                • If something feels wrong, it probably is. Remove yourself from uncomfortable situations immediately.
                
                • Your intuition is a powerful tool developed through years of experience and observation.
                
                • Don't worry about appearing rude - your safety is more important than social conventions.
                
                • Trust your gut feeling when meeting new people or entering unfamiliar places.
                
                • Pay attention to body language and behavior that seems off or threatening.
                
                • If someone makes you uncomfortable, distance yourself without explanation.
                """.trimIndent(),
                "#FF8A65"
            )
        }

        // Awareness Card
        view.findViewById<MaterialCardView>(R.id.card_awareness).setOnClickListener {
            showDetailBottomSheet(
                "Awareness",
                "Stay Alert and Aware",
                """
                • Keep your head up and make eye contact with people around you.
                
                • Avoid using headphones in unfamiliar or isolated areas.
                
                • Be aware of exits and escape routes wherever you go.
                
                • Notice people who seem to be following you or paying unusual attention.
                
                • Stay off your phone when walking, especially at night.
                
                • Walk confidently and with purpose - attackers often target those who appear distracted or vulnerable.
                
                • Trust your peripheral vision and be aware of movements around you.
                """.trimIndent(),
                "#FFB74D"
            )
        }

        // Social Card
        view.findViewById<MaterialCardView>(R.id.card_social).setOnClickListener {
            showDetailBottomSheet(
                "Social",
                "Travel in Groups",
                """
                • There's safety in numbers - travel with friends when possible.
                
                • Use the buddy system, especially at night or in unfamiliar areas.
                
                • If alone, stay in well-populated, well-lit areas.
                
                • Let others know your plans and expected return time.
                
                • Share your live location with trusted friends or family.
                
                • Create a code word with friends to signal when you need help.
                
                • Check in regularly when traveling alone.
                """.trimIndent(),
                "#EF5350"
            )
        }

        // Home Security Card
        view.findViewById<MaterialCardView>(R.id.card_home_security).setOnClickListener {
            showDetailBottomSheet(
                "Home Security",
                "Secure Your Home",
                """
                • Always lock doors and windows, even when you're home.
                
                • Install good lighting around all entrances and dark areas.
                
                • Consider installing a peephole or security camera at your door.
                
                • Don't open the door to strangers - verify identity first.
                
                • Keep curtains closed at night so people can't see inside.
                
                • Vary your routine so potential intruders can't predict your schedule.
                
                • Get to know your neighbors - they can watch out for suspicious activity.
                
                • Consider getting a security system or alarm.
                """.trimIndent(),
                "#EC407A"
            )
        }

        // Transportation Card
        view.findViewById<MaterialCardView>(R.id.card_transportation).setOnClickListener {
            showDetailBottomSheet(
                "Transportation",
                "Safe Rides",
                """
                • Always verify driver details and vehicle number before boarding.
                
                • Share trip details and driver information with trusted contacts.
                
                • Sit in the back seat and keep doors locked during the ride.
                
                • Stay alert and don't fall asleep during the journey.
                
                • Trust your instincts - if you feel uncomfortable, cancel the ride.
                
                • Keep your phone charged and accessible.
                
                • Use the in-app emergency features if available.
                
                • Don't share too much personal information with drivers.
                
                • If the driver takes an unexpected route, question it immediately.
                """.trimIndent(),
                "#FF8A65"
            )
        }

        // Digital Safety Card
        view.findViewById<MaterialCardView>(R.id.card_digital_safety).setOnClickListener {
            showDetailBottomSheet(
                "Digital Safety",
                "Online Privacy",
                """
                • Be cautious about sharing location and personal information on social media.
                
                • Review and adjust privacy settings on all social media accounts.
                
                • Don't accept friend requests or messages from unknown people.
                
                • Avoid posting real-time location updates - share after you've left.
                
                • Be wary of phishing attempts and suspicious links.
                
                • Use strong, unique passwords for different accounts.
                
                • Enable two-factor authentication wherever possible.
                
                • Don't share sensitive information like address, phone number publicly.
                
                • Think before you post - once online, it's hard to remove.
                """.trimIndent(),
                "#FF8A65"
            )
        }
    }

    private fun showDetailBottomSheet(category: String, title: String, details: String, color: String) {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_tip_details, null)

        val categoryTextView = view.findViewById<TextView>(R.id.tv_category)
        val titleTextView = view.findViewById<TextView>(R.id.tv_title)
        val detailsTextView = view.findViewById<TextView>(R.id.tv_details)

        categoryTextView.text = category
        categoryTextView.setTextColor(Color.parseColor(color))
        titleTextView.text = title
        detailsTextView.text = details

        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.show()
    }
}