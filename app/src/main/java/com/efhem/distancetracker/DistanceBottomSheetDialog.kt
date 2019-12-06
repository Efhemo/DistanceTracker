package com.efhem.distancetracker


import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_distance_bottom_sheet_dialog.view.*

/**
 * A simple [Fragment] subclass.
 */
class DistanceBottomSheetDialog : BottomSheetDialogFragment() {

    companion object{
        fun newInstance(distance: Float?, time: String): DistanceBottomSheetDialog? {
            val bottomSheetFragment = DistanceBottomSheetDialog()
            val bundle = Bundle()
            if (distance != null) {
                bundle.putFloat("Distance", distance)
                bundle.putString("Time", time)
                Log.d("Distane", "flo"+distance)
            }
            bottomSheetFragment.setArguments(bundle)
            return bottomSheetFragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_distance_bottom_sheet_dialog, container, false)

        arguments?.let {
            val distance = it.getFloat("Distance")
            val time = it.getString("Time")

            Log.d("Distane 1", "dis "+distance)
            view.distance_convered.text = "$distance m"
            view.time.text = "in $time Min"
        }
        /*val distance = arguments!!.getFloat(
            "Distance"
        ) as Float?*/



        return view
    }





}
