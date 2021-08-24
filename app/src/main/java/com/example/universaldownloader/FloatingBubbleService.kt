package com.example.universaldownloader

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Environment
import android.os.IBinder
import android.util.SparseArray
import android.view.*
import android.view.WindowManager.LayoutParams
import android.widget.ImageView
import android.widget.Toast
import androidx.core.util.keyIterator
import androidx.core.view.isVisible
import at.huber.youtubeExtractor.VideoMeta
import at.huber.youtubeExtractor.YouTubeExtractor
import at.huber.youtubeExtractor.YtFile
import timber.log.Timber
import java.lang.Exception
import kotlin.math.floor

class FloatingBubbleService: Service() {

    private lateinit var floatingBubble: View
    private lateinit var params: LayoutParams
    private lateinit var windowManager: WindowManager

    private var Pointer = PositionParams()

    enum class DownloadType { LOW, MEDIUM, HIGH, AUDIO }
    val DEFAULT_DOWNLOAD_VIDEO: String = "DefaultDownloadVideo-Rename"
    val IS_AUDIO_FILE: Int = -1
    val MIN_VIDEO_QUALITY: Int = 360

    // TODO - Move to Android Room to store desired download type
    private val downloadingType: DownloadType = DownloadType.AUDIO

    override fun onBind(intent: Intent): IBinder? { return null }

    override fun onCreate() {
        super.onCreate()
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        loadFloatingBubble(inflater)
        setOnTouchListeners()
    }

    @SuppressLint("RtlHardcoded")
    private fun loadFloatingBubble(inflater: LayoutInflater) {
        floatingBubble = inflater.inflate(R.layout.bubble_widget_layout, null)

        params = LayoutParams(
             LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            LayoutParams.TYPE_APPLICATION_OVERLAY,
            LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        //Specify the view position
        params.gravity = Gravity.NO_GRAVITY
        params.x = 0
        params.y = 50

        //Add the view to the window
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.addView(floatingBubble, params)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setOnTouchListeners(){
        val bubbleView: View = floatingBubble.findViewById(R.id.bubble_view)
        bubbleView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> storeTouchs(event)
                MotionEvent.ACTION_MOVE -> moveBubble(event)
                MotionEvent.ACTION_UP -> openApp(event)
                else -> false
            }
        }
        val closeButtonCollapsed = floatingBubble.findViewById<ImageView>(R.id.close_btn)
        closeButtonCollapsed.setOnClickListener {
            stopSelf()
        }
    }

    private fun openApp(event: MotionEvent): Boolean {
        val diffPosicaoX = (event.rawX - Pointer.initialTouchX).toInt()
        val diffPosicaoY = (event.rawY - Pointer.initialTouchY).toInt()
        val singleClick: Boolean = diffPosicaoX < 5 && diffPosicaoY < 5

        if (singleClick) {
            val clipboard: ClipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager;
            val clipdata: ClipData? = clipboard.primaryClip
            val item : ClipData.Item? = clipdata?.getItemAt(0);
            val URL = item?.text.toString();
            val intent = Intent(this@FloatingBubbleService, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)

            Toast.makeText(this, URL, Toast.LENGTH_SHORT).show()
            Timber.v("Youtube URL is: %s", item?.text.toString())
            if(item != null ) {
                class YouTubeExtractorURL : YouTubeExtractor(this) {
                    override fun onExtractionComplete(
                        ytFiles: SparseArray<YtFile>?,
                        videoMeta: VideoMeta?
                    ) {
                        ytFiles?.let { files ->
                            if(files.size() == 0) {
                                Timber.d("An issue getting the URL video info")
                                return
                            }
                            findAndDownload(ytFiles, videoMeta);
                        }
                    }
                }
                YouTubeExtractorURL().extract(URL)
            }
            // stopSelf()
        }
        return true
    }

    private fun moveBubble(event: MotionEvent): Boolean {
        with(Pointer) {
            params.x = initialX + (event.rawX - initialTouchX).toInt()
            params.y = initialY + (event.rawY - initialTouchY).toInt()
        }
        windowManager.updateViewLayout(floatingBubble, params)
        return true
    }

    private fun storeTouchs(event: MotionEvent): Boolean {
        with(Pointer) {
            initialX = params.x
            initialY = params.y
            initialTouchX = (event.rawX)
            initialTouchY = (event.rawY)
        }
        return true
    }

    private fun YtFile.goodQualityVideoOrAudioFile(): Boolean{
        return (this.format.height == IS_AUDIO_FILE || this.format.height >= MIN_VIDEO_QUALITY)
    }


    private fun  findAndDownload(ytFiles: SparseArray<YtFile>, vMeta: VideoMeta?){
        val videoTitle = vMeta?.title;
        var filename: String
        var downloadSize: Int?
        val fileInfoArr = arrayListOf<FileInfo>()

        for (itag in ytFiles.keyIterator()){
            val ytFile = ytFiles[itag]
            if (ytFile.goodQualityVideoOrAudioFile()) {
                downloadSize = if (ytFile.format.height != -1) ytFile.format.height else null // store video quality only
                filename = videoTitle + "." + ytFile.format.ext

                if(downloadSize != null){ fileInfoArr.add(FileInfo(filename , ytFile, downloadSize))}

                if(downloadingType == DownloadType.AUDIO){
                    if (ytFile.format.height == -1){
                        downloadFromUrl(FileInfo(filename , ytFile, 0), videoTitle ?: DEFAULT_DOWNLOAD_VIDEO)
                        return
                    }else{
                        Timber.v("Wasn't a audio clip")
                    }
                }
            }
        }

        fileInfoArr.sortBy { it.ytSize }
        var index:Int? = null
        when (downloadingType) {
            DownloadType.LOW ->  index = 0
            DownloadType.MEDIUM->  index = (floor(fileInfoArr.size / 2.0)).toInt()
            DownloadType.HIGH -> index = fileInfoArr.size - 1
            else -> Timber.d("Unrecognized download type %s", downloadingType);
        }
        if(index != null){
            Timber.d(" Download type %s at index at quality %s", downloadingType, fileInfoArr[index].ytSize);
            try{
                downloadFromUrl(fileInfoArr[index], videoTitle ?: "UniversalDownloaderVideo")
            }catch (e : Exception){
                Timber.d(e, "Failure downloading with fileInfoArr at index %d", index)
            }
        }
    }


    private fun downloadFromUrl(fileInfo: FileInfo, videoTitle: String) {
        val uri = Uri.parse(fileInfo.ytFile.url)
        val request = DownloadManager.Request(uri)
        request.setTitle((videoTitle + fileInfo.ytSize.toString()))
        request.allowScanningByMediaScanner()
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileInfo.fileName)
        val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
    }


    override fun onDestroy() {
        super.onDestroy()
        if (floatingBubble.isVisible) windowManager.removeView(floatingBubble)
    }
}

data class FileInfo(val fileName: String, val ytFile: YtFile, val ytSize: Int)
data class PositionParams(
     var initialX: Int = 0,
     var initialY: Int = 0,
     var initialTouchX: Float = 0.toFloat(),
     var initialTouchY: Float = 0.toFloat()
)