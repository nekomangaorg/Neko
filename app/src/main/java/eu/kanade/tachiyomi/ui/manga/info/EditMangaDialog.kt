package eu.kanade.tachiyomi.ui.manga.info

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.bluelinelabs.conductor.Router
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.bumptech.glide.signature.ObjectKey
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import eu.kanade.tachiyomi.data.glide.GlideApp
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.ui.library.LibraryController
import eu.kanade.tachiyomi.util.chop
import eu.kanade.tachiyomi.util.toast
import kotlinx.android.synthetic.main.edit_manga_dialog.view.*
import kotlinx.android.synthetic.main.edit_manga_dialog.view.manga_title
import me.gujun.android.taggroup.TagGroup
import timber.log.Timber
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.IOException
import java.io.InputStream

class EditMangaDialog : DialogController {

    private var dialogView: View? = null

    private val manga: Manga

    private var customCoverUri:Uri? = null

    private val infoController
        get() = targetController as MangaInfoController

    constructor(target: MangaInfoController, manga: Manga) : super(Bundle()
        .apply {
            putLong(KEY_MANGA, manga.id!!)
        }) {
        targetController = target
        this.manga = manga
    }

    @Suppress("unused")
    constructor(bundle: Bundle) : super(bundle) {
        manga = Injekt.get<DatabaseHelper>().getManga(bundle.getLong(KEY_MANGA))
        .executeAsBlocking()!!
    }

    override fun showDialog(router: Router) {
        super.showDialog(router)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        val dialog = MaterialDialog(activity!!).apply {
            customView(viewRes = R.layout.edit_manga_dialog, scrollable = true)
            negativeButton(android.R.string.cancel)
            positiveButton(R.string.action_save) { onPositiveButtonClick() }
        }

        dialogView = dialog.view
        onViewCreated(dialog.view, savedViewState)
        return dialog
    }

    fun onViewCreated(view: View, savedState: Bundle?) {
        GlideApp.with(view.context)
            .asDrawable()
            .load(manga)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .signature(ObjectKey(MangaImpl.getLastCoverFetch(manga.id!!).toString()))
            .dontAnimate()
            .into(view.manga_cover)
        val isLocal = manga.source == LocalSource.ID

        if (isLocal) {
            if (manga.title != manga.url)
                view.manga_title.append(manga.title)
            view.manga_title.hint = "${resources?.getString(R.string.title)}: ${manga.url}"
            view.manga_author.append(manga.author ?: "")
            view.manga_artist.append(manga.artist ?: "")
            view.manga_description.append(manga.description ?: "")
            view.manga_genres_tags.setTags(manga.genre?.split(", ") ?: emptyList())
        }
        else {
            if (manga.currentTitle() != manga.originalTitle())
                view.manga_title.append(manga.currentTitle())
            view.manga_title.hint = "${resources?.getString(R.string.title)}: ${manga
                .originalTitle()}"

            if (manga.currentAuthor() != manga.originalAuthor())
                view.manga_author.append(manga.currentAuthor())
            if (!manga.originalAuthor().isNullOrBlank())
                view.manga_author.hint = "${resources?.getString(R.string
                    .manga_info_author_label)}: ${manga.originalAuthor()}"

            if (manga.currentArtist() != manga.originalArtist())
                view.manga_artist.append(manga.currentArtist())
            if (!manga.originalArtist().isNullOrBlank())
                view.manga_artist.hint = "${resources?.getString(R.string
                    .manga_info_artist_label)}: ${manga.originalArtist()}"

            if (manga.currentDesc() != manga.originalDesc())
                view.manga_description.append(manga.currentDesc())
            if (!manga.originalDesc().isNullOrBlank())
                view.manga_description.hint = "${resources?.getString(R.string.description)}: ${manga
                    .originalDesc()?.chop(15)}"
            if (manga.currentGenres().isNullOrBlank().not()) {
                view.manga_genres_tags.setTags(manga.currentGenres()?.split(", "))
            }
        }
        view.cover_layout.setOnClickListener {
            changeCover()
        }
        view.reset_tags.setOnClickListener { resetTags() }
    }

    private fun resetTags() {
        if (manga.originalGenres().isNullOrBlank() || manga.source == LocalSource.ID)
            dialogView?.manga_genres_tags?.setTags(emptyList())
        else
            dialogView?.manga_genres_tags?.setTags(manga.originalGenres()?.split(", "))
    }

    private fun changeCover() {
        if (manga.favorite) {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(
                Intent.createChooser(intent,
                    resources?.getString(R.string.file_select_cover)),
                101
            )
        } else {
            activity?.toast(R.string.notification_first_add_to_library)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 101) {
            if (data == null || resultCode != Activity.RESULT_OK) return
            val activity = activity ?: return

            try {
                // Get the file's input stream from the incoming Intent
                GlideApp.with(dialogView!!.context)
                    .load(data.data ?: Uri.EMPTY)
                    .into(dialogView!!.manga_cover)
                customCoverUri = data.data
            } catch (error: IOException) {
                activity.toast(R.string.notification_cover_update_failed)
                Timber.e(error)
            }
        }
    }


    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        dialogView = null
    }

    private fun onPositiveButtonClick() {
        infoController.presenter.updateManga(dialogView?.manga_title?.text.toString(),
            dialogView?.manga_author?.text.toString(), dialogView?.manga_artist?.text.toString(),
            customCoverUri, dialogView?.manga_description?.text.toString(),
            dialogView?.manga_genres_tags?.tags)
        infoController.updateTitle()
    }

    private companion object {
        const val KEY_MANGA = "manga_id"
    }

}
