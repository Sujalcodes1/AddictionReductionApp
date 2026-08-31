package com.example.addictionreductionapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.example.addictionreductionapp.databinding.FragmentOnboardPageBinding

class OnboardPageFragment : Fragment() {

    companion object {
        private const val ARG_PAGE = "page_index"
        fun newInstance(i: Int) = OnboardPageFragment().apply {
            arguments = bundleOf(ARG_PAGE to i)
        }
    }

    private val pageIndex by lazy { arguments?.getInt(ARG_PAGE) ?: 0 }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val b = FragmentOnboardPageBinding.inflate(inflater, container, false)
        val d = OnboardingData.pages[pageIndex]

        // Bind data — exact per spec, single teal, no per-page color change
        b.tvNumber.text = d.number
        b.tvTitle1.text = d.title1
        b.tvTitle2.text = d.title2
        b.tvBody.text = d.body
        b.btnNext.text = d.btnText
        b.ivIcon.setImageResource(d.iconRes)

        // Visibility — exact spec (do not change): Skip top per showSkip, Back bottom per showBack
        b.tvSkip.visibility = if (d.showSkip) View.VISIBLE else View.GONE
        b.tvBack.visibility = if (d.showBack) View.VISIBLE else View.GONE
        // Restore Back text/color (was repurposed to Skip)
        b.tvBack.text = getString(R.string.ob_back)
        b.tvBack.setTextColor(ContextCompat.getColor(requireContext(), R.color.ob_back_text))

        // Button width — Rule 10/11 MATCH_PARENT on screen1, WRAP_CONTENT on 2/3/4
        val params = b.btnNext.layoutParams
        params.width = if (!d.showBack)
            ViewGroup.LayoutParams.MATCH_PARENT
        else
            ViewGroup.LayoutParams.WRAP_CONTENT
        b.btnNext.layoutParams = params

        // Dots — uniform 8x8, active teal, inactive grey (Rule 8/9)
        val dots = listOf(b.dot1, b.dot2, b.dot3, b.dot4)
        dots.forEachIndexed { i, dot ->
            dot.setBackgroundResource(
                if (i == pageIndex) R.drawable.bg_dot_active
                else R.drawable.bg_dot_inactive
            )
        }

        // Clicks — ViewBinding only
        b.btnNext.setOnClickListener {
            (activity as? OnboardingActivity)?.goToNext(pageIndex)
        }
        b.tvBack.setOnClickListener {
            (activity as? OnboardingActivity)?.goBack(pageIndex)
        }
        b.tvSkip.setOnClickListener {
            (activity as? OnboardingActivity)?.skipOnboarding()
        }

        return b.root
    }
}
