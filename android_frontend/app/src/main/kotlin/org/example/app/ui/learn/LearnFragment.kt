package org.example.app.ui.learn

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import org.example.app.R

class LearnFragment : Fragment(R.layout.fragment_learn) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = view.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager)

        val pages = listOf(
            "Tutorial" to TutorialFragment(),
            "Tatweej" to TatweejFragment()
        )

        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = pages.size
            override fun createFragment(position: Int): Fragment = pages[position].second
        }

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = pages[position].first
        }.attach()
    }
}
