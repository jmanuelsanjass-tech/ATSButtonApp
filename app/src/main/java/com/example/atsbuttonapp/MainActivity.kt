package com.example.atsbuttonapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.atsbuttonapp.ui.theme.ATSButtonAppTheme
import com.example.atsbuttonapp.ui.theme.DarkBg
import com.example.atsbuttonapp.ui.theme.GamerGold
import com.example.atsbuttonapp.ui.theme.GlassWhite
import com.example.atsbuttonapp.ui.theme.SoftBlue
import com.example.atsbuttonapp.ui.theme.SoftCyan
import com.example.atsbuttonapp.ui.theme.SoftPurple
import com.example.atsbuttonapp.ui.theme.SurfaceDark
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .writeTimeout(2, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
        .retryOnConnectionFailure(false)
        .build()

    // Configuración por defecto basada en tus capturas
    private var serverUrlState = mutableStateOf("http://192.168.1.3:4000/command")
    private var isConnectedState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Intentar conectar al inicio
        testConnection()

        setContent {
            ATSButtonAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val url by remember { serverUrlState }
                    val connected by remember { isConnectedState }
                    
                    ATSButtonScreen(
                        onCommand = { key -> sendCommand(key) },
                        serverUrl = url,
                        isConnected = connected,
                        onUpdateIp = { newIp -> 
                            serverUrlState.value = "http://$newIp:4000/command"
                            testConnection()
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun testConnection() {
        lifecycleScope.launch(Dispatchers.IO) {
            val checkUrl = serverUrlState.value.substringBeforeLast("/") // Probar la raíz del server
            val request = Request.Builder().url(checkUrl).build()
            try {
                client.newCall(request).execute().use { response ->
                    withContext(Dispatchers.Main) {
                        isConnectedState.value = true
                        Log.d("ATSConnection", "Servidor detectado correctamente")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isConnectedState.value = false
                    Log.e("ATSConnection", "No se pudo conectar: ${e.message}")
                }
            }
        }
    }

    private fun sendCommand(key: String) {
        val json = "{\"key\":\"$key\"}"
        val body = json.toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder().url(serverUrlState.value).post(body).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ATSCommand", "Error de envío: ${e.message}")
                runOnUiThread { isConnectedState.value = false }
            }
            override fun onResponse(call: Call, response: Response) {
                response.close()
                runOnUiThread { isConnectedState.value = true }
            }
        })
    }
}

@Composable
fun ATSButtonScreen(
    onCommand: (String) -> Unit, 
    serverUrl: String,
    isConnected: Boolean,
    onUpdateIp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSplash by remember { mutableStateOf(true) }
    // Extraer la IP actual de la URL
    var inputIp by remember { mutableStateOf(serverUrl.substringAfter("//").substringBefore(":")) }
    
    var lightsOn by remember { mutableStateOf(false) }
    var highBeamsOn by remember { mutableStateOf(false) }
    var engineBrakeOn by remember { mutableStateOf(false) }
    var parkingBrakeOn by remember { mutableStateOf(false) }
    var electronicsOn by remember { mutableStateOf(false) }
    var strobosOn by remember { mutableStateOf(false) }
    var trailerOn by remember { mutableStateOf(false) }
    var cruiseOn by remember { mutableStateOf(false) }
    var retarderOn by remember { mutableStateOf(false) }
    var incCruiseOn by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(2000)
        showSplash = false
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(painter = painterResource(id = R.drawable.fondo), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)

        AnimatedVisibility(visible = showSplash, enter = fadeIn(), exit = fadeOut(tween(800))) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painter = painterResource(id = R.drawable.boton1), contentDescription = null, modifier = Modifier.size(200.dp).clip(CircleShape).border(2.dp, SoftCyan, CircleShape))
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("INICIANDO CONSOLA...", color = SoftCyan, fontFamily = FontFamily.Monospace)
                }
            }
        }

        AnimatedVisibility(visible = !showSplash, enter = fadeIn(tween(1000))) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                
                // IP Setup
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(0.85f)) {
                    OutlinedTextField(
                        value = inputIp,
                        onValueChange = { inputIp = it },
                        label = { Text("IP de tu PC", color = Color.White.copy(0.6f)) },
                        modifier = Modifier.weight(1f).height(65.dp),
                        textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SoftCyan,
                            unfocusedBorderColor = Color.Gray,
                            cursorColor = SoftCyan
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = { onUpdateIp(inputIp) },
                        colors = ButtonDefaults.buttonColors(containerColor = SoftCyan.copy(0.2f)),
                        modifier = Modifier.height(55.dp).border(1.dp, SoftCyan, RoundedCornerShape(8.dp))
                    ) {
                        Text("OK", color = SoftCyan, fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = "P A N E L   -   S J B o y s .....",
                    style = TextStyle(color = SoftPurple, fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, shadow = Shadow(SoftPurple, blurRadius = 10f)),
                    textAlign = TextAlign.Center
                )

                Card(
                    modifier = Modifier.fillMaxWidth().shadow(20.dp, RoundedCornerShape(32.dp), spotColor = SoftBlue),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Brush.verticalGradient(listOf(SoftBlue.copy(0.5f), Color.Transparent)))
                ) {
                    Column(modifier = Modifier.padding(8.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val btnMod = Modifier.weight(1f).aspectRatio(1f)
                        Row(modifier = Modifier.fillMaxWidth(0.95f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            GamerButton("", R.drawable.play, if(lightsOn) Color.Green else Color.Red, isActive = lightsOn, modifier = btnMod, onClick = { lightsOn=!lightsOn; onCommand("L") })
                            GamerButton("", R.drawable.playlargo, if(highBeamsOn) Color.Green else Color.Red, isActive = highBeamsOn, modifier = btnMod, onClick = { highBeamsOn=!highBeamsOn; onCommand("Q") })
                            GamerButton("", R.drawable.aremotor, if(engineBrakeOn) Color.Green else Color.Red, isActive = engineBrakeOn, modifier = btnMod, onClick = { engineBrakeOn=!engineBrakeOn; onCommand("E") })
                            GamerButton("", R.drawable.arenoesta, if(parkingBrakeOn) Color.Green else Color.Red, isActive = parkingBrakeOn, modifier = btnMod, onClick = { parkingBrakeOn=!parkingBrakeOn; onCommand("R") })
                            GamerButton("", R.drawable.electro, if(electronicsOn) Color.Green else Color.Red, isActive = electronicsOn, modifier = btnMod, onClick = { electronicsOn=!electronicsOn; onCommand("W") })
                        }
                        Row(modifier = Modifier.fillMaxWidth(0.95f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            GamerButton("", R.drawable.estroboti, if(strobosOn) Color.Green else Color.Red, isActive = strobosOn, modifier = btnMod, onClick = { strobosOn=!strobosOn; onCommand("U") })
                            GamerButton("", R.drawable.conectdesconecta, if(trailerOn) Color.Green else Color.Red, isActive = trailerOn, modifier = btnMod, onClick = { trailerOn=!trailerOn; onCommand("O") })
                            GamerButton("", R.drawable.crucero, if(cruiseOn) Color.Green else Color.Red, isActive = cruiseOn, modifier = btnMod, onClick = { cruiseOn=!cruiseOn; onCommand("P") })
                            GamerButton("", R.drawable.retarder, if(retarderOn) Color.Green else Color.Red, isActive = retarderOn, modifier = btnMod, onClick = { retarderOn=!retarderOn; onCommand("P") })
                            GamerButton("", R.drawable.aumentocrucero, if(incCruiseOn) Color.Green else Color.Red, isActive = incCruiseOn, modifier = btnMod, onClick = { incCruiseOn=!incCruiseOn; onCommand("Z") })
                        }
                    }
                }

                Text(
                    text = if (isConnected) "• CONECTADO •" else "• DESCONECTADO •",
                    style = TextStyle(
                        color = if (isConnected) Color.Green else Color.Red,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        shadow = Shadow(if(isConnected) Color.Green else Color.Red, blurRadius = 5f)
                    )
                )
            }
        }
    }
}

@Composable
fun GamerButton(
    text: String,
    iconRes: Int,
    accentColor: Color,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.92f else 1f)
    val glow by animateDpAsState(if (isActive) 20.dp else 10.dp)

    Card(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(glow, CircleShape, ambientColor = accentColor, spotColor = accentColor)
            .clip(CircleShape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .border(2.dp, Brush.verticalGradient(listOf(Color.White.copy(0.4f), Color.Transparent, Color.Black.copy(0.4f))), CircleShape),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(0.6f))
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Image(painter = painterResource(id = iconRes), contentDescription = null, modifier = Modifier.fillMaxSize(1f).clip(CircleShape).graphicsLayer(alpha = if(isActive) 1f else 0.6f), contentScale = ContentScale.FillBounds)
            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp).width(15.dp).height(3.dp).background(if(isActive) accentColor else Color.Gray, CircleShape))
        }
    }
}
