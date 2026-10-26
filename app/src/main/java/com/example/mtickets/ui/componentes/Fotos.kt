package com.example.mtickets.ui.componentes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

fun crearArchivoFoto(context: Context): File {
    val carpeta = File(context.filesDir, "fotos").apply { mkdirs() }
    return File(carpeta, "IMG_${System.currentTimeMillis()}_${(0..9999).random()}.jpg")
}

fun uriDeArchivo(context: Context, archivo: File): Uri {
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
}

fun copiarDeGaleria(context: Context, origen: Uri): String? {
    return try {
        val destino = crearArchivoFoto(context)
        context.contentResolver.openInputStream(origen)?.use { entrada ->
            destino.outputStream().use { salida -> entrada.copyTo(salida) }
        }
        destino.absolutePath
    } catch (e: Exception) {
        null
    }
}

fun cargarBitmap(ruta: String, maxLado: Int = 900): ImageBitmap? {
    return try {
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(ruta, limites)
        if (limites.outWidth <= 0 || limites.outHeight <= 0) return null

        var muestreo = 1
        while (maxOf(limites.outWidth, limites.outHeight) / muestreo > maxLado) muestreo *= 2

        val opciones = BitmapFactory.Options().apply { inSampleSize = muestreo }
        var bitmap: Bitmap = BitmapFactory.decodeFile(ruta, opciones) ?: return null

        val orientacion = ExifInterface(ruta).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val grados = when (orientacion) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (grados != 0f) {
            val matriz = Matrix().apply { postRotate(grados) }
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matriz, true)
        }
        bitmap.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}

@Composable
fun ImagenArchivo(
    ruta: String,
    modifier: Modifier = Modifier,
    escala: ContentScale = ContentScale.Crop,
    descripcion: String? = null
) {
    val imagen by produceState<ImageBitmap?>(initialValue = null, ruta) {
        value = withContext(Dispatchers.IO) { cargarBitmap(ruta) }
    }
    val bitmap = imagen

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = descripcion,
            modifier = modifier,
            contentScale = escala
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.BrokenImage, contentDescription = null)
        }
    }
}

@Composable
fun GaleriaEvidencias(titulo: String, rutas: List<String>) {
    if (rutas.isEmpty()) return

    var ampliada by remember { mutableStateOf<String?>(null) }

    SeccionCard(titulo) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            rutas.forEach { ruta ->
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { ampliada = ruta }
                ) {
                    ImagenArchivo(ruta = ruta, modifier = Modifier.size(110.dp), descripcion = "Fotografía")
                }
            }
        }
    }

    val rutaAmpliada = ampliada
    if (rutaAmpliada != null) {
        AlertDialog(
            onDismissRequest = { ampliada = null },
            confirmButton = {
                TextButton(onClick = { ampliada = null }) { Text("Cerrar") }
            },
            text = {
                ImagenArchivo(
                    ruta = rutaAmpliada,
                    modifier = Modifier.fillMaxWidth(),
                    escala = ContentScale.Fit,
                    descripcion = "Fotografía ampliada"
                )
            }
        )
    }
}

@Composable
fun SelectorFotos(
    rutas: List<String>,
    onCambio: (List<String>) -> Unit,
    maximo: Int = 4
) {
    val context = LocalContext.current

    var pendiente by rememberSaveable { mutableStateOf<String?>(null) }

    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { exito ->
        val ruta = pendiente
        if (ruta != null) {
            if (exito) onCambio(rutas + ruta) else File(ruta).delete()
        }
        pendiente = null
    }

    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val ruta = copiarDeGaleria(context, uri)
            if (ruta != null) {
                onCambio(rutas + ruta)
            } else {
                Toast.makeText(context, "No se pudo cargar la imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val puedeAgregar = rutas.size < maximo

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                enabled = puedeAgregar,
                onClick = {
                    val archivo = crearArchivoFoto(context)
                    pendiente = archivo.absolutePath
                    try {
                        camara.launch(uriDeArchivo(context, archivo))
                    } catch (e: Exception) {
                        pendiente = null
                        Toast.makeText(context, "No se pudo abrir la cámara", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Text("  Tomar foto")
            }

            OutlinedButton(
                enabled = puedeAgregar,
                onClick = {
                    galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                Text("  Galería")
            }
        }

        Text(
            text = "Máximo $maximo fotos. Evita mostrar rostros, documentos o datos personales.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (rutas.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rutas.forEach { ruta ->
                    Box(modifier = Modifier.size(90.dp)) {
                        ImagenArchivo(
                            ruta = ruta,
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            descripcion = "Foto seleccionada"
                        )
                        IconButton(
                            onClick = {
                                File(ruta).delete()
                                onCambio(rutas - ruta)
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(28.dp)
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Quitar foto",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
