package com.bignerdranch.android.criminalintent

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import java.util.Date

class CrimeFragment : Fragment() {

    private lateinit var crime: Crime
    private lateinit var titleField: EditText
    private lateinit var detailsField: EditText
    private lateinit var dateButton: Button
    private lateinit var solvedCheckBox: CheckBox
    private lateinit var suspectButton: Button
    private lateinit var callButton: Button
    private lateinit var reportButton: Button
    private var rootView: View? = null

    companion object {
        private const val REQUEST_CONTACT = 1
        private const val DATE_FORMAT = "EEE, MMM dd"
        private const val REPORT_DATE_TIME_FORMAT = "EEE, MMM dd, HH:mm"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crime = Crime()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_crime, container, false)
        rootView = view

        titleField = view.findViewById(R.id.crime_title)
        detailsField = view.findViewById(R.id.crime_details)
        dateButton = view.findViewById(R.id.crime_date)
        solvedCheckBox = view.findViewById(R.id.crime_solved)
        suspectButton = view.findViewById(R.id.crime_suspect)
        callButton = view.findViewById(R.id.crime_call)
        reportButton = view.findViewById(R.id.crime_report)

        dateButton.apply {
            text = crime.date.toString()
            isEnabled = false
        }

        return view
    }

    override fun onStart() {
        super.onStart()

        val titleWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                crime.title = s.toString()
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        titleField.addTextChangedListener(titleWatcher)

        val detailsWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                crime.details = s.toString()
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        detailsField.addTextChangedListener(detailsWatcher)

        solvedCheckBox.setOnCheckedChangeListener { _, isChecked ->
            crime.isSolved = isChecked
            rootView?.let {
                Snackbar.make(it, R.string.checkbox_snack, Snackbar.LENGTH_SHORT).show()
            }
        }

        suspectButton.setOnClickListener {
            val pickContactIntent =
                Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
            startActivityForResult(pickContactIntent, REQUEST_CONTACT)
        }

        callButton.setOnClickListener {
            if (crime.phone.isNotBlank()) {
                val phoneUri = Uri.parse("tel:${crime.phone}")
                val intent = Intent(Intent.ACTION_DIAL, phoneUri)
                startActivity(intent)
            } else {
                rootView?.let {
                    Snackbar.make(it, R.string.error_no_phone, Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        reportButton.setOnClickListener {
            if (crime.title.isBlank()) {
                rootView?.let {
                    Snackbar.make(it, R.string.error_empty_title, Snackbar.LENGTH_SHORT).show()
                }
                return@setOnClickListener
            }

            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getCrimeReport())
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crime_report_subject))
            }.also { intent ->
                val chooserIntent = Intent.createChooser(intent, getString(R.string.send_report))
                startActivity(chooserIntent)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when {
            resultCode != Activity.RESULT_OK -> return
            requestCode == REQUEST_CONTACT && data != null -> {
                val contactUri: Uri? = data.data
                val queryFields = arrayOf(
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts._ID
                )

                if (contactUri != null) {
                    val cursor = requireActivity().contentResolver
                        .query(contactUri, queryFields, null, null, null)

                    cursor?.use {
                        if (it.count == 0) return
                        it.moveToFirst()

                        val suspect = it.getString(0)
                        val contactId = it.getString(1)

                        crime.suspect = suspect
                        suspectButton.text = suspect

                        val phoneCursor = requireActivity().contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                            arrayOf(contactId),
                            null
                        )

                        phoneCursor?.use { pCursor ->
                            if (pCursor.moveToFirst()) {
                                val numberIndex =
                                    pCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                if (numberIndex >= 0) {
                                    val number = pCursor.getString(numberIndex)
                                    crime.phone = number
                                    callButton.isEnabled = true
                                    callButton.text = "Позвонить подозреваемому ($number)"
                                }
                            } else {
                                crime.phone = ""
                                callButton.isEnabled = false
                                callButton.text = "Позвонить подозреваемому"
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getCrimeReport(): String {
        val solvedString = if (crime.isSolved) {
            getString(R.string.crime_report_solved)
        } else {
            getString(R.string.crime_report_unsolved)
        }

        val dateWithTime = DateFormat.format(REPORT_DATE_TIME_FORMAT, crime.date).toString()

        val suspect = if (crime.suspect.isBlank()) {
            getString(R.string.crime_report_no_suspect)
        } else {
            getString(R.string.crime_report_suspect, crime.suspect)
        }

        return getString(
            R.string.crime_report,
            crime.title,
            dateWithTime,
            solvedString,
            suspect,
            crime.details
        )
    }
}