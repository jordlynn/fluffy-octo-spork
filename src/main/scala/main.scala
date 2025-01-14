import java.io._
import javax.swing.JFileChooser
import swing._
import event._

    /*
        TODO
        disable dpms and screensaver for a certain amount of time
        save and store different monitor configurations
        save monitor state as x config file
        optionally lock together brightnesses
        cmd arg to launch in popup mode at the cursor
        Get the swing components look nicer and match the monitor widget
    */
object main extends SimpleSwingApplication{
    val monitors = Monitor.get().filter(x=>x.connected)
    def top = new MainFrame {
        peer.setType(java.awt.Window.Type.UTILITY)
        title = "Fluffy octo spork"
        contents = new BoxPanel(Orientation.Vertical) {
            val positionMover = new positionWidget(monitors)
            val refreshDisplay : ()=>Unit = () => {
                positionMover.refresh()
            }
            contents += positionMover
            for(monitor <- monitors){
                println(monitor)
                def setRes(x: Res): Unit = {
                    monitor.resolution = x
                    refreshDisplay()
                }
                def setRot(x: Rotation): Unit = {
                    monitor.rotation = x
                    refreshDisplay()
                }
                def setRef(x: Reflection): Unit = {
                    monitor.reflection = x
                    refreshDisplay()
                }
                def setBrightness(d: Double): Unit = {
                    monitor.brightness = d
                    refreshDisplay()
                }
                def setEnabled(e: Boolean): Unit = {
                    monitor.enabled = e
                    refreshDisplay()
                }
                val res = for(res <- monitor.resList) yield {
                    res.toString -> (() => setRes(res))
                }
                val state = {
                    val l = List(
                            ("On", ()=>setEnabled(true)),
                            ("Off", ()=>setEnabled(false))
                        )
                    if(monitor.enabled) l else l.reverse
                }
                val parameters :List[parameter] = List(
                    new choiceList(state),
                    new choiceList(res),
                    new caseParam(monitor.rotation, Rotation.all, setRot(_:Rotation)),
                    new caseParam(monitor.reflection, Reflection.all, setRef(_:Reflection)),
                    new Range(setBrightness(_), start = 1.0, step = 0.1)
                    )
                contents += new BoxPanel(Orientation.Horizontal) {
                    contents ++= parameters.map(x => x.getComponent)
                }
            }
            contents += new BoxPanel(Orientation.Horizontal) {
                val maxButtonSize = new Dimension(8000,8000)
                val ApplyButton = new Button("Apply"){
                    maximumSize = maxButtonSize
                }
                val SaveButton = new Button("Save"){
                    maximumSize = maxButtonSize
                }
                listenTo(ApplyButton)
                listenTo(SaveButton)
                reactions += {
                    case ButtonClicked(ApplyButton) => {
                        for(monitor <- monitors) monitor.publish()
                        refreshDisplay()
                    }
                    case ButtonClicked(SaveButton) => {
                        val fb = new JFileChooser()
                        val result = fb.showOpenDialog(null)
                        if(result == JFileChooser.APPROVE_OPTION) {
                            val file = fb.getSelectedFile()
                            val command = "#!/bin/bash\n"+
                              monitors.map(m => m.getCommand()).mkString("\n")
                            val writer = new PrintWriter(file)
                            writer.print(command)
                            writer.close()
                            file.setExecutable(true)
                        }
                    }
                }
                contents += ApplyButton
                contents += SaveButton
            }
        }
    }
}

