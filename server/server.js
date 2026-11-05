const express = require('express');
const robot = require('robotjs');
const dgram = require('dgram');
const os = require('os');
const app = express();
const port = 3000;
const discoveryPort = 4000;

// Obtener la IP local de la PC
function getLocalIP() {
    const interfaces = os.networkInterfaces();
    for (const name of Object.keys(interfaces)) {
        for (const iface of interfaces[name]) {
            if (iface.family === 'IPv4' && !iface.internal) {
                return iface.address;
            }
        }
    }
    return '127.0.0.1';
}

const localIP = getLocalIP();

// Middleware para parsear JSON
app.use(express.json());

// Endpoint de prueba (Health Check)
app.get('/', (req, res) => {
    res.status(200).send("SERVIDOR ATS ONLINE");
});

app.post('/command', (req, res) => {
    const key = req.body.key;
    if (key) {
        console.log(`[${new Date().toLocaleTimeString()}] Tecla: ${key}`);
        try {
            robot.keyTap(key.toLowerCase());
            res.status(200).send({ status: 'success' });
        } catch (error) {
            res.status(500).send({ status: 'error' });
        }
    } else {
        res.status(400).send({ status: 'error' });
    }
});

// --- Descubrimiento UDP ---
const discoveryServer = dgram.createSocket('udp4');

discoveryServer.on('message', (msg, rinfo) => {
    if (msg.toString() === 'DISCOVER_ATS_SERVER') {
        console.log(`Dispositivo encontrado: ${rinfo.address}`);
        const response = Buffer.from(`ATS_SERVER_ACK:${port}`);
        discoveryServer.send(response, rinfo.port, rinfo.address);
    }
});

discoveryServer.on('error', (err) => {
    console.error(`Error UDP: ${err.stack}`);
});

discoveryServer.bind(discoveryPort, '0.0.0.0', () => {
    discoveryServer.setBroadcast(true);
    console.log(`Servicio de Autodescubrimiento activo (Puerto ${discoveryPort})`);
});

// --- Iniciar Servidor HTTP ---
app.listen(port, '0.0.0.0', () => {
    console.log(`\n==========================================`);
    console.log(`SERVIDOR ATS INICIADO`);
    console.log(`IP de tu PC: ${localIP}`);
    console.log(`URL de comando: http://${localIP}:${port}/command`);
    console.log(`==========================================\n`);
    console.log(`1. Asegúrate que el móvil esté en el mismo Wi-Fi.`);
    console.log(`2. Si no conecta, REVISA EL FIREWALL de Windows.`);
});
