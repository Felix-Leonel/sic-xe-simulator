    package Ligador;

    import Mem.Memoria;
    import Mem.Palavramem;
    import Carregador.AbsoluteLoader;
    import java.io.BufferedReader;
    import java.io.FileReader;
    import java.io.IOException;
    import Regs.Registradores;
    import java.util.HashMap;
    import java.util.Map;

    public class Ligador {
        
        private static final String OBJECT_FILE = "teste.txt"; 
        private static int EXECADDR; // Endereço de execução
        private Map<String, Integer> ESTAB = new HashMap<>(); // Tabela de símbolos externos
        private String programName;

        public static void main(String[] args) {
            Memoria memoria = new Memoria();  
            Ligador ligador = new Ligador();
            
            memoria.memoria.get(0).setValor((byte) 0x01, (byte) 0x02, (byte) 0x03);
            memoria.memoria.get(1).setValor((byte) 0x0A, (byte) 0x0B, (byte) 0x0C);
            
            System.out.println("--- Memória antes da execução ---");
            ligador.printMemory(memoria, 0, 10);
            
            ligador.pass1();
            ligador.pass2(memoria);
            
            System.out.println("--- Memória depois da execução ---");
            ligador.printMemory(memoria, 0, 200);
        }

        public void pass1() {
            try (BufferedReader file = new BufferedReader(new FileReader(OBJECT_FILE))) {
                String register;
                
                while ((register = file.readLine()) != null) {
                    String[] parts = register.split("\\^");
                    char type = parts[0].charAt(0);
        
                    if (type == 'H') {  // Cabeçalho
                        programName = parts[1].trim();
                    } 
                    else if (type == 'D') {  // Definição de símbolo externo
                        String symbol = parts[1].trim();
                        int address = Integer.parseInt(parts[2].trim(), 16) % 1000; // Garantir que está no intervalo de 0 a 999
                        ESTAB.put(symbol, address);
                        System.out.println("Passagem 1 - Definição de símbolo: " + symbol + " -> " + address);
                    } 
                }
            } catch (IOException e) {
                System.err.println("Erro na leitura do arquivo objeto: " + e.getMessage());
            } catch (NumberFormatException e) {
                System.err.println("Erro ao converter número hexadecimal: " + e.getMessage());
            }
        }
        
        public void pass2(Memoria memoria) {
            try (BufferedReader file = new BufferedReader(new FileReader(OBJECT_FILE))) {
                String register;
                Registradores registradores = new Registradores();
                
                while ((register = file.readLine()) != null) {
                    String[] parts = register.split("\\^");
                    char type = parts[0].charAt(0);
        
                    if (type == 'H') {  // Cabeçalho
                        int firstFreeAddress = findNextFreeMemoryIndex(memoria);
                        System.out.println("Passagem 2 - Carregando segmento " + programName + " no primeiro espaço livre: " + firstFreeAddress);
                    } 
                    else if (type == 'T') { 
                        int address = findNextFreeMemoryIndex(memoria);
                        StringBuilder code = new StringBuilder();
                        for (int i = 3; i < parts.length; i++) {
                            code.append(parts[i]);
                        }
                        moveToMemory(memoria, address, code.toString());
                        System.out.println("Passagem 2 - Trecho de código carregado no endereço " + address + ": " + code);
                    } 
                    else if (type == 'M') { 
                        int address = Integer.parseInt(parts[1].trim(), 16) % 1000; // Ajuste no intervalo
                        if (parts.length >= 4) {
                            String symbol = parts[3].trim();
                            if (!ESTAB.containsKey(symbol)) {
                                System.err.println("Erro: Símbolo indefinido " + symbol);
                                continue;
                            }
                            int modification = ESTAB.get(symbol);
                            if (address < memoria.memoria.size()) {
                                memoria.updateMemory(address, modification);
                                System.out.println("Passagem 2 - Relocando símbolo " + symbol + " no endereço " + address + " com valor " + modification);
                            } else {
                                System.err.println("Erro: Endereço de memória inválido " + address);
                            }
                        } else {
                            System.err.println("Erro: Formato inválido na linha de modificação!");
                        }
                    } 
                    else if (type == 'E') { 
                        if (parts.length > 1) {
                            try {
                                EXECADDR = Integer.parseInt(parts[1].trim(), 16) % 1000; // Ajuste no intervalo
                            } catch (NumberFormatException e) {
                                System.err.println("Aviso: Endereço de execução inválido, usando padrão.");
                                EXECADDR = 0;
                            }
                        } else {
                            EXECADDR = 0;
                        }
                    }
                }
                
                System.out.println("\nTabela de Símbolos Externos:");
                for (Map.Entry<String, Integer> entry : ESTAB.entrySet()) {
                    System.out.println(entry.getKey() + " -> " + entry.getValue());
                }
                
                AbsoluteLoader loader = new AbsoluteLoader(memoria, registradores);
                loader.executeAtAddress(EXECADDR);
        
            } catch (IOException e) {
                System.err.println("Erro na leitura do arquivo objeto: " + e.getMessage());
            } catch (NumberFormatException e) {
                System.err.println("Erro ao converter número hexadecimal: " + e.getMessage());
            }
        }
        
        public void printMemory(Memoria memoria, int start, int count) {
            for (int i = start; i < start + count && i < memoria.memoria.size(); i++) {
                Palavramem palavra = memoria.memoria.get(i);
                byte[] bytes = palavra.getBytes();
                System.out.printf("Endereço %d: %02X %02X %02X\n", i, bytes[0], bytes[1], bytes[2]);
            }
        }
        
        private int findNextFreeMemoryIndex(Memoria memoria) {
            for (int i = 0; i < memoria.memoria.size(); i++) {
                Palavramem palavra = memoria.memoria.get(i);
                if (palavra.getBytes()[0] == 0 && palavra.getBytes()[1] == 0 && palavra.getBytes()[2] == 0) {
                    return i;
                }
            }
            return memoria.memoria.size();
        }
        
        public void moveToMemory(Memoria memoria, int address, String code) {
            for (int i = 0; i < code.length(); i += 6) {
                int memIndex = findNextFreeMemoryIndex(memoria);
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
    }
