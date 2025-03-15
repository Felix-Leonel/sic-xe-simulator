package Carregador;

import Mem.Memoria;
import Mem.Palavramem;
import Regs.Registradores;
import java.io.*;

public class AbsoluteLoader {
    private static final String OBJECT_FILE = "object_code.txt";
    private Memoria memoria;
    private Registradores registradores;

    public AbsoluteLoader(Memoria memoria, Registradores registradores) {
        this.memoria = memoria;
        this.registradores = registradores;
    }

    public static void main(String[] args) {
        Memoria memoria = new Memoria();
        Registradores registradores = new Registradores();
    
        memoria.memoria.get(0).setValor((byte) 0x01, (byte) 0x02, (byte) 0x03);
        memoria.memoria.get(1).setValor((byte) 0x0A, (byte) 0x0B, (byte) 0x0C);
    
        AbsoluteLoader loader = new AbsoluteLoader(memoria, registradores);
        loader.execute();
    }
    

    public void execute() {
        System.out.println("Memória antes da execução:");
        printMemory(15);

        loadModule(OBJECT_FILE);

        System.out.println("\nMemória após a execução:");
        printMemory(15);
    }

    public void loadModule(String module) {
        try (BufferedReader file = new BufferedReader(new FileReader(module))) {
            String register;
            while ((register = file.readLine()) != null) {
                String[] parts = register.split("\\^");
                char type = parts[0].charAt(0);

                if (type == 'H') {
                    System.out.println("Header encontrado: " + register);
                } else if (type == 'T') {
                    processTextRecord(parts);
                } else if (type == 'E') {
                    jumpToAddress(findFirstUsedAddress());
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao abrir ou ler o arquivo: " + e.getMessage());
        }
    }

    private void processTextRecord(String[] parts) {
        StringBuilder code = new StringBuilder();
        for (int i = 3; i < parts.length; i++) {
            code.append(parts[i]);
        }
        moveToMemory(code.toString());
    }

    private void moveToMemory(String code) {
        int memIndex;
        for (int i = 0; i < code.length(); i += 6) {
            memIndex = findNextFreeMemoryIndex();
            if (memIndex >= memoria.memoria.size()) {
                System.err.println("Erro: Memória insuficiente!");
                return;
            }

            if (i + 6 <= code.length()) {
                byte b1 = (byte) Integer.parseInt(code.substring(i, i + 2), 16);
                byte b2 = (byte) Integer.parseInt(code.substring(i + 2, i + 4), 16);
                byte b3 = (byte) Integer.parseInt(code.substring(i + 4, i + 6), 16);
                memoria.memoria.get(memIndex).setValor(b1, b2, b3);
            }
        }
    }

    private void jumpToAddress(int address) {
        if (address >= memoria.memoria.size()) {
            System.err.printf("Erro: Endereço de execução %06X está fora da memória!\n", address);
            return;
        }

        Palavramem instrucao = memoria.memoria.get(address);
        registradores.getRegistradores(0).setReg(instrucao.getBytes()[0], instrucao.getBytes()[1], instrucao.getBytes()[2]);
        //System.out.printf("Executando código a partir do endereço %d\n", address);
    }

    private void printMemory(int limit) {
        System.out.println("\nConteúdo da Memória:");
        for (int i = 0; i < Math.min(limit, memoria.memoria.size()); i++) {
            Palavramem palavra = memoria.memoria.get(i);
            System.out.printf("Endereço %d: %02X %02X %02X\n", i, palavra.getBytes()[0], palavra.getBytes()[1], palavra.getBytes()[2]);
        }
    }

    private int findNextFreeMemoryIndex() {
        for (int i = 0; i < memoria.memoria.size(); i++) {
            Palavramem palavra = memoria.memoria.get(i);
            if (palavra.getBytes()[0] == 0 && palavra.getBytes()[1] == 0 && palavra.getBytes()[2] == 0) {
                return i;
            }
        }
        return memoria.memoria.size();
    }

    private int findFirstUsedAddress() {
        for (int i = 0; i < memoria.memoria.size(); i++) {
            Palavramem palavra = memoria.memoria.get(i);
            if (palavra.getBytes()[0] != 0 || palavra.getBytes()[1] != 0 || palavra.getBytes()[2] != 0) {
                return i;
            }
        }
        return 0;
    }
}
