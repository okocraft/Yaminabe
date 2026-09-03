package net.okocraft.yaminabe.paper.command;

import net.okocraft.yaminabe.paper.platform.EntityScheduler;
import net.okocraft.yaminabe.paper.testsupport.CommandTester;
import net.okocraft.yaminabe.paper.testsupport.TestSources;
import org.bukkit.WeatherType;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PWeatherCommandPermissionTest {

    private static final String PERMISSION = "yaminabe.command.pweather";
    private static final String SET_PERMISSION = PERMISSION + ".set";
    private static final String SET_OTHERS_PERMISSION = SET_PERMISSION + ".others";
    private static final String RESET_PERMISSION = PERMISSION + ".reset";
    private static final String RESET_OTHERS_PERMISSION = RESET_PERMISSION + ".others";
    private static final String QUERY_PERMISSION = PERMISSION + ".query";
    private static final String QUERY_OTHERS_PERMISSION = QUERY_PERMISSION + ".others";

    private static final EntityScheduler DIRECT_SCHEDULER = (entity, task) -> {
        task.run();
        return true;
    };

    private final CommandTester tester = CommandTester.of(PWeatherCommand.createPWeatherCommand(DIRECT_SCHEDULER));

    private Player sender;
    private Player executor;

    @BeforeEach
    void setUp() {
        this.sender = Mockito.mock(Player.class);
        this.executor = Mockito.mock(Player.class);
        Mockito.when(this.sender.getName()).thenReturn("Sender");
        Mockito.when(this.executor.getName()).thenReturn("Executor");
        TestSources.grant(this.sender, PERMISSION, RESET_PERMISSION, QUERY_PERMISSION, SET_OTHERS_PERMISSION);
    }

    @Test
    void testSetOthersPermissionDoesNotAllowResettingAnotherPlayer() throws Exception {
        Assertions.assertEquals(0, this.tester.execute(TestSources.of(this.sender, this.executor), "pweather reset"));

        Mockito.verify(this.executor, Mockito.never()).resetPlayerWeather();
        Mockito.verify(this.sender).sendMessage(CommandMessages.PWEATHER_OTHERS_PREVENTED);
    }

    @Test
    void testResetOthersPermissionAllowsResettingAnotherPlayer() throws Exception {
        TestSources.grant(this.sender, RESET_OTHERS_PERMISSION);

        Assertions.assertEquals(1, this.tester.execute(TestSources.of(this.sender, this.executor), "pweather reset"));

        Mockito.verify(this.executor).resetPlayerWeather();
        Mockito.verify(this.sender).sendMessage(CommandMessages.PWEATHER_RESET.apply("Executor"));
    }

    @Test
    void testSetOthersPermissionDoesNotAllowQueryingAnotherPlayer() throws Exception {
        Assertions.assertEquals(0, this.tester.execute(TestSources.of(this.sender, this.executor), "pweather query"));

        Mockito.verify(this.executor, Mockito.never()).getPlayerWeather();
        Mockito.verify(this.sender).sendMessage(CommandMessages.PWEATHER_OTHERS_PREVENTED);
    }

    @Test
    void testQueryOthersPermissionAllowsQueryingAnotherPlayer() throws Exception {
        Mockito.when(this.executor.getPlayerWeather()).thenReturn(WeatherType.CLEAR);
        TestSources.grant(this.sender, QUERY_OTHERS_PERMISSION);

        Assertions.assertEquals(1, this.tester.execute(TestSources.of(this.sender, this.executor), "pweather query"));

        Mockito.verify(this.sender).sendMessage(CommandMessages.PWEATHER_QUERY_FIXED.apply("Executor", "clear"));
    }
}
