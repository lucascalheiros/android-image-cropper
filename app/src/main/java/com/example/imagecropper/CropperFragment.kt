package com.example.imagecropper

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.imagecropper.databinding.FragmentCropperBinding

class CropperFragment : Fragment() {

    private var photoUri: String? = null

    private lateinit var binding: FragmentCropperBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            photoUri = it.getString(ARG_PHOTO_URI)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentCropperBinding.inflate(layoutInflater, container, false)

        Glide
            .with(requireContext())
            .load(photoUri)
            .into(binding.ivPhoto)

        return binding.root
    }

    companion object {
        const val ARG_PHOTO_URI = "ARG_PHOTO_URI"

        @JvmStatic
        fun newInstance(photoUri: String) =
            CropperFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PHOTO_URI, photoUri)
                }
            }
    }
}