import { ArVrPlugin } from 'capacitor-ar-vr-plugin';

window.testEcho = () => {
    const inputValue = document.getElementById("echoInput").value;
    ArVrPlugin.echo({ value: inputValue })
}
