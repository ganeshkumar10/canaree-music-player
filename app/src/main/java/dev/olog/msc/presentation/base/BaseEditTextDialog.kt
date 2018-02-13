package dev.olog.msc.presentation.base

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.design.widget.TextInputEditText
import android.support.design.widget.TextInputLayout
import android.view.View
import android.view.animation.AnimationUtils
import dev.olog.msc.R
import dev.olog.msc.presentation.utils.ImeUtils
import dev.olog.msc.utils.k.extension.makeDialog
import dev.olog.msc.utils.k.extension.unsubscribe
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import org.jetbrains.anko.toast
import java.util.concurrent.TimeUnit

abstract class BaseEditTextDialog : BaseDialogFragment() {

    private var hideKeyboardDisposable: Disposable? = null
    private val errorDisposable : Disposable? = null

    private lateinit var clearButton : View
    private lateinit var editText : TextInputEditText

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val application = activity!!.application

        val builder = AlertDialog.Builder(context)
                .setTitle(title())
                .setView(R.layout.layout_edit_text)
                .setNegativeButton(negativeButtonMessage(application), null)
                .setPositiveButton(positiveButtonMessage(application), null)

        val dialog = builder.makeDialog()

        editText = dialog.findViewById(R.id.editText)
        val editTextLayout = dialog.findViewById<TextInputLayout>(R.id.editTextLayout)
        clearButton = dialog.findViewById(R.id.clear)

        editText.setText(initialTextFieldValue())

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    val editTextString = editText.text.toString()

                    if (editTextString.isBlank()){
                        showError(editTextLayout, errorMessageForBlankForm())
                    } else if (!isStringValid(editTextString)){
                        showError(editTextLayout, errorMessageForInvalidForm(editTextString))
                    } else {
                        positiveAction(editTextString)
                                .observeOn(AndroidSchedulers.mainThread())
                                .doOnComplete { application.toast(successMessage(application, editTextString)) }
                                .doOnError { application.toast(negativeMessage(application, editTextString)) }
                                .subscribe()
                        dismiss()
                    }
                }

        hideKeyboardDisposable = Observable.timer(500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ ImeUtils.showIme(editText) }, Throwable::printStackTrace)

        return dialog
    }

    private fun showError(editTextLayout: TextInputLayout, @StringRes errorStringId: Int){
        val shake = AnimationUtils.loadAnimation(context, R.anim.shake)
        editTextLayout.startAnimation(shake)
        editTextLayout.error = getString(errorStringId)
        editTextLayout.isErrorEnabled = true

        hideKeyboardDisposable.unsubscribe()
        hideKeyboardDisposable = Single.timer(2, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ editTextLayout.isErrorEnabled = false }, Throwable::printStackTrace)
    }

    override fun onResume() {
        super.onResume()
        clearButton.setOnClickListener { editText.setText("") }
    }

    override fun onPause() {
        super.onPause()
        clearButton.setOnClickListener(null)
    }

    override fun onStop() {
        super.onStop()
        hideKeyboardDisposable.unsubscribe()
        errorDisposable.unsubscribe()
    }

    protected abstract fun title(): Int

    protected abstract fun negativeButtonMessage(context: Context) : Int
    protected abstract fun positiveButtonMessage(context: Context) : Int

    protected abstract fun errorMessageForBlankForm() : Int
    protected abstract fun errorMessageForInvalidForm(currentValue: String) : Int

    protected abstract fun isStringValid(string: String): Boolean
    protected abstract fun initialTextFieldValue(): String

    protected abstract fun successMessage(context: Context, currentValue: String): CharSequence
    protected abstract fun negativeMessage(context: Context, currentValue: String): CharSequence

    protected abstract fun positiveAction(currentValue: String) : Completable


}