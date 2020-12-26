#!/usr/bin/env jbang
//DEPS info.picocli:picocli:4.5.0
//DEPS info.picocli:picocli-codegen:4.5.0
//DEPS io.fabric8:kubernetes-client:4.13.0
//DEPS com.massisframework:j-text-utils:0.3.4

import dnl.utils.text.table.TextTable;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.PodStatusUtil;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Command(name = "lp", mixinStandardHelpOptions = true, version = "lp 0.1",
        description = "list pods made with jbang")
class lp implements Callable<Integer> {

    private static final String CHECK_MARK = "\u2705";
    private static final String FIRE = "\uD83D\uDD25";

    public static void main(String... args) {
        int exitCode = new CommandLine(new lp()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        printTable(getPods());
        return 0;
    }

    private static List<PodInfo> getPods() {
        KubernetesClient kc;
        try {
            kc = new DefaultKubernetesClient();
        } catch (Exception e) {
            throw new RuntimeException("Unable to create default Kubernetes client", e);
        }

        return kc.pods().list().getItems().stream().map(pod -> {
            PodInfoState state = PodStatusUtil.isRunning(pod) ? PodInfoState.RUNNING : PodInfoState.FAILING;
            String message = null;
            if (!state.equals(PodInfoState.RUNNING)) {
                message = PodStatusUtil.getContainerStatus(pod).get(0).getState().getWaiting().getMessage();
            }

            return new PodInfo(pod.getMetadata().getName(), state, message);
        }).collect(Collectors.toList());
    }

    static class PodInfo {

        private final String name;
        private final PodInfoState state;
        private final String message;

        public PodInfo(String name, PodInfoState state, String message) {
            this.name = name;
            this.state = state;
            this.message = message;
        }

        public String getName() {
            return name;
        }

        public PodInfoState getState() {
            return state;
        }

        public String getMessage() {
            return message;
        }
    }

    enum PodInfoState {
        RUNNING,
        FAILING
    }

    private static void printTable(List<PodInfo> list) {
        final Object[][] tableData = list.stream()
                .map(podInfo -> new Object[]{
                        podInfo.getState().equals(PodInfoState.RUNNING) ? CHECK_MARK : FIRE,
                        podInfo.getName(),
                        podInfo.getState(),
                        podInfo.getMessage()
                })
                .toArray(Object[][]::new);
        String[] columnNames = {"", "name", "state", "message"};
        new TextTable(columnNames, tableData).printTable();
    }
}
