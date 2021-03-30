package eu.kanade.tachiyomi.ui.manga

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import coil.api.loadAny
import coil.request.Parameters
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.image.coil.MangaFetcher
import eu.kanade.tachiyomi.databinding.EditMangaDialogBinding
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.util.lang.chop
import eu.kanade.tachiyomi.util.view.visibleIf
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class EditMangaDialog : DialogController {

    private val manga: Manga

    private var customCoverUri: Uri? = null

    private var willResetCover = false

    lateinit var binding: EditMangaDialogBinding

    private val infoController
        get() = targetController as MangaDetailsController

    constructor(target: MangaDetailsController, manga: Manga) : super(
        Bundle()
            .apply {
                putLong(KEY_MANGA, manga.id!!)
            }
    ) {
        targetController = target
        this.manga = manga
    }

    @Suppress("unused")
    constructor(bundle: Bundle) : super(bundle) {
        manga = Injekt.get<DatabaseHelper>().getManga(bundle.getLong(KEY_MANGA))
            .executeAsBlocking()!!
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        val dialog = MaterialDialog(activity!!).apply {
            customView(viewRes = R.layout.edit_manga_dialog, scrollable = true)
            negativeButton(android.R.string.cancel)
            positiveButton(R.string.save) { onPositiveButtonClick() }
        }
        binding = EditMangaDialogBinding.bind(dialog.getCustomView())
        onViewCreated()
        dialog.setOnShowListener {
            val dView = (it as? MaterialDialog)?.view
            dView?.contentLayout?.scrollView?.scrollTo(0, 0)
        }
        return dialog
    }

    fun onViewCreated() {
        binding.mangaCover.loadAny(manga)
        val isLocal = manga.source == LocalSource.ID

        if (isLocal) {
            if (manga.title != manga.url) {
                binding.title.append(manga.title)
            }
            binding.title.hint = "${resources?.getString(R.string.title)}: ${manga.url}"
            binding.mangaAuthor.append(manga.author ?: "")
            binding.mangaArtist.append(manga.artist ?: "")
            binding.mangaDescription.append(manga.description ?: "")
            binding.mangaGenresTags.setTags(manga.genre?.split(", ") ?: emptyList())
        } else {
            if (manga.title != manga.originalTitle) {
                binding.title.append(manga.title)
            }
            if (manga.author != manga.originalAuthor) {
                binding.mangaAuthor.append(manga.author ?: "")
            }
            if (manga.artist != manga.originalArtist) {
                binding.mangaArtist.append(manga.artist ?: "")
            }
            if (manga.description != manga.originalDescription) {
                binding.mangaDescription.append(manga.description ?: "")
            }
            binding.mangaGenresTags.setTags(manga.genre?.split(", ") ?: emptyList())

            binding.title.hint = "${resources?.getString(R.string.title)}: ${manga.originalTitle}"
            if (manga.originalAuthor != null) {
                binding.mangaAuthor.hint = "${resources?.getString(R.string.author)}: ${manga.originalAuthor}"
            }
            if (manga.originalArtist != null) {
                binding.mangaArtist.hint = "${resources?.getString(R.string.artist)}: ${manga.originalArtist}"
            }
            if (manga.originalDescription != null) {
                binding.mangaDescription.hint =
                    "${resources?.getString(R.string.description)}: ${manga.originalDescription?.replace(
                        "\n",
                        " "
                    )?.chop(20)}"
            }
        }
        binding.mangaGenresTags.clearFocus()
        binding.coverLayout.setOnClickListener {
            infoController.changeCover()
        }
        binding.resetTags.setOnClickListener { resetTags() }
        binding.resetCover.visibleIf(!isLocal)
        binding.resetCover.setOnClickListener {
            binding.mangaCover.loadAny(
                manga,
                builder = {
                    parameters(Parameters.Builder().set(MangaFetcher.realCover, true).build())
                }
            )
            willResetCover = true
        }
    }

    private fun resetTags() {
        if (manga.genre.isNullOrBlank() || manga.source == LocalSource.ID) binding.mangaGenresTags.setTags(
            emptyList()
        )
        else binding.mangaGenresTags.setTags(manga.originalGenre?.split(", "))
    }

    fun updateCover(uri: Uri) {
        willResetCover = false
        binding.mangaCover.loadAny(uri)
        customCoverUri = uri
    }

    private fun onPositiveButtonClick() {
        infoController.presenter.updateManga(
            binding.title.text.toString(),
            binding.mangaAuthor.text.toString(),
            binding.mangaArtist.text.toString(),
            customCoverUri,
            binding.mangaDescription.text.toString(),
            binding.mangaGenresTags.tags,
            willResetCover
        )
    }

    private companion object {
        const val KEY_MANGA = "manga_id"
    }
}
