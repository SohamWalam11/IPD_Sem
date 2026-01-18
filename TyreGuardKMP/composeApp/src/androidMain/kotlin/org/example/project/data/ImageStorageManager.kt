package org.example.project.data

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manages saving and loading tyre images to internal storage.
 * 
 * Images are saved as .jpg files in the app's private storage directory
 * under the "tyre_images" folder.
 */
class ImageStorageManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ImageStorageManager"
        private const val IMAGE_DIRECTORY = "tyre_images"
        private const val IMAGE_QUALITY = 90
    }
    
    private val imageDir: File by lazy {
        File(context.filesDir, IMAGE_DIRECTORY).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    
    /**
     * Save a bitmap as a .jpg file and return the file path.
     * 
     * @param bitmap The image to save
     * @param prefix Optional prefix for the filename (e.g., "analysis")
     * @return Absolute path to the saved image, or null if save failed
     */
    fun saveImage(bitmap: Bitmap, prefix: String = "tyre"): String? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val filename = "${prefix}_${timestamp}.jpg"
            val file = File(imageDir, filename)
            
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, outputStream)
                outputStream.flush()
            }
            
            Log.d(TAG, "Image saved: ${file.absolutePath}")
            file.absolutePath
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save image: ${e.message}", e)
            null
        }
    }
    
    /**
     * Load an image from the given file path.
     * 
     * @param imagePath Absolute path to the image file
     * @return Bitmap if loaded successfully, null otherwise
     */
    fun loadImage(imagePath: String): Bitmap? {
        return try {
            val file = File(imagePath)
            if (file.exists()) {
                android.graphics.BitmapFactory.decodeFile(imagePath)
            } else {
                Log.w(TAG, "Image file not found: $imagePath")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load image: ${e.message}", e)
            null
        }
    }
    
    /**
     * Delete an image from storage.
     * 
     * @param imagePath Absolute path to the image file
     * @return true if deleted successfully, false otherwise
     */
    fun deleteImage(imagePath: String): Boolean {
        return try {
            val file = File(imagePath)
            if (file.exists()) {
                file.delete().also { deleted ->
                    if (deleted) {
                        Log.d(TAG, "Image deleted: $imagePath")
                    }
                }
            } else {
                true // Already deleted
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete image: ${e.message}", e)
            false
        }
    }
    
    /**
     * Get all saved image paths.
     * 
     * @return List of absolute paths to all saved images
     */
    fun getAllImagePaths(): List<String> {
        return imageDir.listFiles()
            ?.filter { it.isFile && it.extension.equals("jpg", ignoreCase = true) }
            ?.map { it.absolutePath }
            ?.sortedDescending()
            ?: emptyList()
    }
    
    /**
     * Get total storage used by images in bytes.
     */
    fun getStorageUsedBytes(): Long {
        return imageDir.listFiles()
            ?.filter { it.isFile }
            ?.sumOf { it.length() }
            ?: 0L
    }
    
    /**
     * Delete all saved images.
     * 
     * @return Number of images deleted
     */
    fun clearAllImages(): Int {
        var count = 0
        imageDir.listFiles()?.forEach { file ->
            if (file.isFile && file.delete()) {
                count++
            }
        }
        Log.d(TAG, "Cleared $count images")
        return count
    }
}
