import Mem.Memoria;
import Regs.Registradores;
import Carregador.AbsoluteLoader;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {
    private Memoria memoria;
    private Registradores registradores;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Inicializa Memória e Registradores
        memoria = new Memoria();
        registradores = new Registradores();

        // Inicializa memória com valores específicos
        memoria.memoria.get(0).setValor((byte) 0x01, (byte) 0x02, (byte) 0x03);
        memoria.memoria.get(1).setValor((byte) 0x0A, (byte) 0x0B, (byte) 0x0C);

        // Cria o AbsoluteLoader e executa a carga do programa
        AbsoluteLoader loader = new AbsoluteLoader(memoria, registradores);
        loader.execute();

        // Carrega a interface gráfica
        FXMLLoader loaderFXML = new FXMLLoader(getClass().getResource("style.fxml"));
        Parent root = loaderFXML.load();

        Controller controller = loaderFXML.getController();
        controller.setStage(primaryStage);
        controller.updateRegistradores(registradores);
        controller.updateMemoria(memoria);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("styless.css").toExternalForm());
        primaryStage.setTitle("Simulador SIC");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
