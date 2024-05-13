# Copyright (c) Microsoft. All rights reserved.

import os
import sys
from typing import Union
from unittest.mock import AsyncMock, patch

import pytest

from semantic_kernel import Kernel
from semantic_kernel.connectors.ai.chat_completion_client_base import ChatCompletionClientBase
from semantic_kernel.connectors.ai.prompt_execution_settings import PromptExecutionSettings
from semantic_kernel.connectors.openai_plugin.openai_function_execution_parameters import (
    OpenAIFunctionExecutionParameters,
)
from semantic_kernel.events.function_invoked_event_args import FunctionInvokedEventArgs
from semantic_kernel.events.function_invoking_event_args import FunctionInvokingEventArgs
from semantic_kernel.exceptions import (
    KernelFunctionAlreadyExistsError,
    KernelServiceNotFoundError,
    ServiceInvalidTypeError,
)
from semantic_kernel.exceptions.kernel_exceptions import KernelFunctionNotFoundError, KernelPluginNotFoundError
from semantic_kernel.exceptions.template_engine_exceptions import TemplateSyntaxError
from semantic_kernel.functions.function_result import FunctionResult
from semantic_kernel.functions.kernel_arguments import KernelArguments
from semantic_kernel.functions.kernel_function_decorator import kernel_function
from semantic_kernel.functions.kernel_plugin import KernelPlugin
from semantic_kernel.services.ai_service_client_base import AIServiceClientBase
from semantic_kernel.services.ai_service_selector import AIServiceSelector


# region Init
def test_init():
    kernel = Kernel()
    assert kernel is not None
    assert kernel.ai_service_selector is not None
    assert kernel.plugins is not None
    assert kernel.services is not None
    assert kernel.retry_mechanism is not None
    assert kernel.function_invoked_handlers is not None
    assert kernel.function_invoking_handlers is not None


def test_kernel_init_with_ai_service_selector():
    ai_service_selector = AIServiceSelector()
    kernel = Kernel(ai_service_selector=ai_service_selector)
    assert kernel.ai_service_selector is not None


def test_kernel_init_with_services(service: AIServiceClientBase):
    kernel = Kernel(services=service)
    assert kernel.services is not None
    assert kernel.services["service"] is not None


def test_kernel_init_with_services_dict(service: AIServiceClientBase):
    kernel = Kernel(services={"service": service})
    assert kernel.services is not None
    assert kernel.services["service"] is not None


def test_kernel_init_with_services_list(service: AIServiceClientBase):
    kernel = Kernel(services=[service])
    assert kernel.services is not None
    assert kernel.services["service"] is not None


def test_kernel_init_with_plugins():
    plugins = {"plugin": KernelPlugin(name="plugin")}
    kernel = Kernel(plugins=plugins)
    assert kernel.plugins is not None


# endregion
# region Invoke Functions


@pytest.mark.asyncio
async def test_invoke_function(kernel: Kernel, create_mock_function):
    mock_function = create_mock_function(name="test_function")

    await kernel.invoke(mock_function, KernelArguments())

    assert mock_function.invoke.call_count == 1


@pytest.mark.asyncio
async def test_invoke_functions_by_name(kernel: Kernel, create_mock_function):
    mock_function = create_mock_function(name="test_function")
    kernel.add_plugin(KernelPlugin(name="test", functions=[mock_function]))

    await kernel.invoke(function_name="test_function", plugin_name="test", arguments=KernelArguments())

    assert mock_function.invoke.call_count == 1

    async for response in kernel.invoke_stream(function_name="test_function", plugin_name="test"):
        assert response[0].text == "test"


@pytest.mark.asyncio
async def test_invoke_function_fail(kernel: Kernel, create_mock_function):
    mock_function = create_mock_function(name="test_function")
    kernel.add_plugin(KernelPlugin(name="test", functions=[mock_function]))

    with pytest.raises(KernelFunctionNotFoundError):
        await kernel.invoke(arguments=KernelArguments())

    with pytest.raises(KernelFunctionNotFoundError):
        async for _ in kernel.invoke_stream(arguments=KernelArguments()):
            pass


@pytest.mark.asyncio
async def test_invoke_stream_function(kernel: Kernel, create_mock_function):
    mock_function = create_mock_function(name="test_function")
    kernel.add_plugin(KernelPlugin(name="test", functions=[mock_function]))

    async for part in kernel.invoke_stream(mock_function, input="test"):
        assert part[0].text == "test"

    assert mock_function.invoke.call_count == 0


@pytest.mark.asyncio
async def test_invoke_stream_functions_throws_exception(kernel: Kernel, create_mock_function):
    mock_function = create_mock_function(name="test_function")
    kernel.add_plugin(KernelPlugin(name="test", functions=[mock_function]))
    functions = [mock_function]

    function_result_with_exception = FunctionResult(
        value="", function=mock_function.metadata, output=None, metadata={"exception": "Test Exception"}
    )

    with patch("semantic_kernel.kernel.Kernel.invoke_stream", return_value=AsyncMock()) as mocked_invoke_stream:
        mocked_invoke_stream.return_value.__aiter__.return_value = [function_result_with_exception]

        async for part in kernel.invoke_stream(functions, input="test"):
            assert "exception" in part.metadata, "Expected exception metadata in the FunctionResult."
            assert part.metadata["exception"] == "Test Exception", "The exception message does not match."
            break


@pytest.mark.asyncio
async def test_invoke_prompt(kernel: Kernel, create_mock_function):
    mock_function = create_mock_function(name="test_function")
    with patch(
        "semantic_kernel.functions.kernel_function_from_prompt.KernelFunctionFromPrompt._invoke_internal"
    ) as mock_invoke:
        mock_invoke.return_value = mock_function.invoke.return_value
        await kernel.invoke_prompt(prompt="test", plugin_name="test", function_name="test", arguments=KernelArguments())
        mock_invoke.assert_called_once()


@pytest.mark.asyncio
async def test_invoke_prompt_no_prompt_error(kernel: Kernel):
    with pytest.raises(TemplateSyntaxError):
        await kernel.invoke_prompt(
            function_name="test_function",
            plugin_name="test_plugin",
            prompt="",
        )


# endregion
# region Function Invoking/Invoked Events


def test_invoke_handles_register(kernel_with_handlers: Kernel):
    assert len(kernel_with_handlers.function_invoking_handlers) == 1
    assert len(kernel_with_handlers.function_invoked_handlers) == 1


def test_invoke_handles_remove(kernel_with_handlers: Kernel):
    assert len(kernel_with_handlers.function_invoking_handlers) == 1
    assert len(kernel_with_handlers.function_invoked_handlers) == 1

    invoking_handler = list(kernel_with_handlers.function_invoking_handlers.values())[0]
    invoked_handler = list(kernel_with_handlers.function_invoked_handlers.values())[0]

    kernel_with_handlers.remove_function_invoking_handler(invoking_handler)
    kernel_with_handlers.remove_function_invoked_handler(invoked_handler)

    assert len(kernel_with_handlers.function_invoking_handlers) == 0
    assert len(kernel_with_handlers.function_invoked_handlers) == 0


@pytest.mark.asyncio
async def test_invoke_handles_pre_invocation(kernel: Kernel, create_mock_function):
    mock_function = create_mock_function(name="test_function")
    kernel.add_plugin(KernelPlugin(name="test", functions=[mock_function]))

    invoked = 0

    def invoking_handler(kernel: Kernel, e: FunctionInvokingEventArgs) -> FunctionInvokingEventArgs:
        nonlocal invoked
        invoked += 1
        return e

    kernel.add_function_invoking_handler(invoking_handler)

    # Act
    await kernel.invoke(mock_function, KernelArguments())

    # Assert
    assert invoked == 1
    assert mock_function.invoke.call_count == 1


@pytest.mark.asyncio
async def test_invoke_handles_post_invocation(kernel: Kernel, create_mock_function):
    mock_function = create_mock_function("test_function")
    invoked = 0

    def invoked_handler(sender, e):
        nonlocal invoked
        invoked += 1
        return e

    kernel.add_function_invoked_handler(invoked_handler)

    # Act
    _ = await kernel.invoke(mock_function, KernelArguments())

    # Assert
    assert invoked == 1
    mock_function.invoke.assert_called()
    assert mock_function.invoke.call_count == 1


@pytest.mark.asyncio
async def test_invoke_post_invocation_repeat_is_working(kernel: Kernel, create_mock_function):
    mock_function = create_mock_function(name="RepeatMe")
    invoked = 0
    repeat_times = 0

    def invoked_handler(sender, e):
        nonlocal invoked, repeat_times
        invoked += 1

        if repeat_times < 3:
            e.repeat()
            repeat_times += 1
        return e

    kernel.add_function_invoked_handler(invoked_handler)

    # Act
    _ = await kernel.invoke(mock_function)

    # Assert
    assert invoked == 4
    assert repeat_times == 3


@pytest.mark.asyncio
async def test_invoke_change_variable_invoking_handler(kernel: Kernel, create_mock_function):
    original_input = "Importance"
    new_input = "Problems"

    mock_function = create_mock_function(name="test_function", value=new_input)

    def invoking_handler(sender, e: FunctionInvokingEventArgs):
        e.arguments["input"] = new_input
        e.updated_arguments = True
        return e

    kernel.add_function_invoking_handler(invoking_handler)
    arguments = KernelArguments(input=original_input)
    # Act
    result = await kernel.invoke(mock_function, arguments)

    # Assert
    assert str(result) == new_input
    assert arguments["input"] == new_input


@pytest.mark.asyncio
async def test_invoke_change_variable_invoked_handler(kernel: Kernel, create_mock_function):
    original_input = "Importance"
    new_input = "Problems"

    mock_function = create_mock_function(name="test_function", value=new_input)

    def invoked_handler(sender, e: FunctionInvokedEventArgs):
        e.arguments["input"] = new_input
        e.updated_arguments = True
        return e

    kernel.add_function_invoked_handler(invoked_handler)
    arguments = KernelArguments(input=original_input)

    # Act
    result = await kernel.invoke(mock_function, arguments)

    # Assert
    assert str(result) == new_input
    assert arguments["input"] == new_input


# endregion
# region Plugins


def test_add_plugin_from_directory(kernel: Kernel):
    # import plugins
    plugins_directory = os.path.join(os.path.dirname(__file__), "../../assets", "test_plugins")
    # path to plugins directory
    plugin = kernel.add_plugin(plugin_name="TestPlugin", parent_directory=plugins_directory)
    assert plugin is not None
    assert len(plugin.functions) == 2
    func = plugin.functions["TestFunction"]
    assert func is not None
    func_handlebars = plugin.functions["TestFunctionHandlebars"]
    assert func_handlebars is not None


def test_plugin_no_plugin(kernel: Kernel):
    with pytest.raises(ValueError):
        kernel.add_plugin(plugin_name="test")


def test_plugin_name_error(kernel: Kernel):
    with pytest.raises(ValueError):
        kernel.add_plugin(" ", None)


def test_plugins_add_plugins(kernel: Kernel):
    plugin1 = KernelPlugin(name="TestPlugin")
    plugin2 = KernelPlugin(name="TestPlugin2")

    kernel.add_plugins([plugin1, plugin2])
    assert len(kernel.plugins) == 2


def test_add_function_from_prompt(kernel: Kernel):
    prompt = """
    Write a short story about two Corgis on an adventure.
    The story must be:
    - G rated
    - Have a positive message
    - No sexism, racism or other bias/bigotry
    - Be exactly {{$paragraph_count}} paragraphs long
    - Be written in this language: {{$language}}
    - The two names of the corgis are {{GenerateNames.generate_names}}
    """

    kernel.add_function(
        prompt=prompt,
        function_name="TestFunction",
        plugin_name="TestPlugin",
        description="Write a short story.",
        execution_settings=PromptExecutionSettings(
            extension_data={"max_tokens": 500, "temperature": 0.5, "top_p": 0.5}
        ),
    )
    func = kernel.get_function("TestPlugin", "TestFunction")
    assert func.name == "TestFunction"
    assert func.description == "Write a short story."
    assert len(func.parameters) == 2


def test_add_function_not_provided(kernel: Kernel):
    with pytest.raises(ValueError):
        kernel.add_function(function_name="TestFunction", plugin_name="TestPlugin")


def test_add_functions(kernel: Kernel):
    @kernel_function(name="func1")
    def func1(arg1: str) -> str:
        return "test"

    @kernel_function(name="func2")
    def func2(arg1: str) -> str:
        return "test"

    plugin = kernel.add_functions(plugin_name="test", functions=[func1, func2])
    assert len(plugin.functions) == 2


def test_add_functions_to_existing(kernel: Kernel):
    kernel.add_plugin(KernelPlugin(name="test"))

    @kernel_function(name="func1")
    def func1(arg1: str) -> str:
        return "test"

    @kernel_function(name="func2")
    def func2(arg1: str) -> str:
        return "test"

    plugin = kernel.add_functions(plugin_name="test", functions=[func1, func2])
    assert len(plugin.functions) == 2


@pytest.mark.asyncio
@patch("semantic_kernel.connectors.openai_plugin.openai_utils.OpenAIUtils.parse_openai_manifest_for_openapi_spec_url")
async def test_add_plugin_from_openai(mock_parse_openai_manifest, kernel: Kernel):
    base_folder = os.path.join(os.path.dirname(__file__), "../../assets/test_plugins")
    with open(os.path.join(base_folder, "TestOpenAIPlugin", "akv-openai.json"), "r") as file:
        openai_spec = file.read()

    openapi_spec_file_path = os.path.join(
        os.path.dirname(__file__), base_folder, "TestOpenAPIPlugin", "akv-openapi.yaml"
    )
    mock_parse_openai_manifest.return_value = openapi_spec_file_path

    await kernel.add_plugin_from_openai(
        plugin_name="TestOpenAIPlugin",
        plugin_str=openai_spec,
        execution_parameters=OpenAIFunctionExecutionParameters(
            http_client=AsyncMock(),
            auth_callback=AsyncMock(),
            server_url_override="http://localhost",
            enable_dynamic_payload=True,
        ),
    )
    plugin = kernel.plugins["TestOpenAIPlugin"]
    assert plugin is not None
    assert plugin.name == "TestOpenAIPlugin"
    assert plugin.functions.get("GetSecret") is not None
    assert plugin.functions.get("SetSecret") is not None


def test_import_plugin_from_openapi(kernel: Kernel):
    openapi_spec_file = os.path.join(
        os.path.dirname(__file__), "../../assets/test_plugins", "TestOpenAPIPlugin", "akv-openapi.yaml"
    )

    kernel.add_plugin_from_openapi(
        plugin_name="TestOpenAPIPlugin",
        openapi_document_path=openapi_spec_file,
    )
    plugin = kernel.plugins["TestOpenAPIPlugin"]
    assert plugin is not None
    assert plugin.name == "TestOpenAPIPlugin"
    assert plugin.functions.get("GetSecret") is not None
    assert plugin.functions.get("SetSecret") is not None


def test_get_plugin(kernel: Kernel):
    kernel.add_plugin(KernelPlugin(name="TestPlugin"))
    plugin = kernel.get_plugin("TestPlugin")
    assert plugin is not None


def test_get_plugin_not_found(kernel: Kernel):
    with pytest.raises(KernelPluginNotFoundError):
        kernel.get_plugin("TestPlugin2")


def test_get_function(kernel: Kernel, custom_plugin_class):
    kernel.add_plugin(custom_plugin_class(), "TestPlugin")
    func = kernel.get_function("TestPlugin", "getLightStatus")
    assert func


def test_func_plugin_not_found(kernel: Kernel):
    with pytest.raises(KernelPluginNotFoundError):
        kernel.get_function("TestPlugin", "TestFunction")


def test_func_function_not_found(kernel: Kernel, custom_plugin_class):
    kernel.add_plugin(custom_plugin_class(), "TestPlugin")
    with pytest.raises(KernelFunctionNotFoundError):
        kernel.get_function("TestPlugin", "TestFunction")


def test_get_function_from_fqn(kernel: Kernel, custom_plugin_class):
    kernel.add_plugin(custom_plugin_class(), "TestPlugin")
    func = kernel.get_function_from_fully_qualified_function_name("TestPlugin-getLightStatus")
    assert func


def test_get_function_from_fqn_wo_plugin(kernel: Kernel, custom_plugin_class):
    kernel.add_plugin(custom_plugin_class(), "TestPlugin")
    func = kernel.get_function_from_fully_qualified_function_name("getLightStatus")
    assert func


# endregion
# region Services


def test_kernel_add_service(kernel: Kernel, service: AIServiceClientBase):
    kernel.add_service(service)
    assert kernel.services == {"service": service}


def test_kernel_add_service_twice(kernel_with_service: Kernel, service: AIServiceClientBase):
    with pytest.raises(KernelFunctionAlreadyExistsError):
        kernel_with_service.add_service(service)
    assert kernel_with_service.services == {"service": service}


def test_kernel_add_multiple_services(kernel_with_service: Kernel, service: AIServiceClientBase):
    service2 = AIServiceClientBase(service_id="service2", ai_model_id="ai_model_id")
    kernel_with_service.add_service(service2)
    assert kernel_with_service.services["service2"] == service2
    assert len(kernel_with_service.services) == 2


def test_kernel_remove_service(kernel_with_service: Kernel):
    kernel_with_service.remove_service("service")
    assert kernel_with_service.services == {}


def test_kernel_remove_service_error(kernel_with_service: Kernel):
    with pytest.raises(KernelServiceNotFoundError):
        kernel_with_service.remove_service("service2")


def test_kernel_remove_all_service(kernel_with_service: Kernel):
    kernel_with_service.remove_all_services()
    assert kernel_with_service.services == {}


def test_get_default_service(kernel_with_default_service: Kernel):
    service_get = kernel_with_default_service.get_service()
    assert service_get == kernel_with_default_service.services["default"]


def test_get_default_service_with_type(kernel_with_default_service: Kernel):
    service_get = kernel_with_default_service.get_service(type=AIServiceClientBase)
    assert service_get == kernel_with_default_service.services["default"]


def test_get_service(kernel_with_service: Kernel):
    service_get = kernel_with_service.get_service("service")
    assert service_get == kernel_with_service.services["service"]


def test_get_service_by_type(kernel_with_service: Kernel):
    service_get = kernel_with_service.get_service(type=AIServiceClientBase)
    assert service_get == kernel_with_service.services["service"]


def test_get_service_by_type_not_found(kernel_with_service: Kernel):
    with pytest.raises(KernelServiceNotFoundError):
        kernel_with_service.get_service(type=ChatCompletionClientBase)


def test_get_default_service_by_type(kernel_with_default_service: Kernel):
    service_get = kernel_with_default_service.get_services_by_type(AIServiceClientBase)
    assert service_get["default"] == kernel_with_default_service.services["default"]


def test_get_services_by_type(kernel_with_service: Kernel):
    service_get = kernel_with_service.get_services_by_type(AIServiceClientBase)
    assert service_get["service"] == kernel_with_service.services["service"]


def test_get_service_with_id_not_found(kernel_with_service: Kernel):
    with pytest.raises(KernelServiceNotFoundError):
        kernel_with_service.get_service("service2", type=AIServiceClientBase)


def test_get_service_with_type(kernel_with_service: Kernel):
    service_get = kernel_with_service.get_service("service", type=AIServiceClientBase)
    assert service_get == kernel_with_service.services["service"]


def test_get_service_with_multiple_types(kernel_with_service: Kernel):
    service_get = kernel_with_service.get_service("service", type=(AIServiceClientBase, ChatCompletionClientBase))
    assert service_get == kernel_with_service.services["service"]


@pytest.mark.skipif(sys.version_info < (3, 10), reason="This is valid syntax only in python 3.10+.")
def test_get_service_with_multiple_types_union(kernel_with_service: Kernel):
    """This is valid syntax only in python 3.10+. It is skipped for older versions."""
    service_get = kernel_with_service.get_service("service", type=Union[AIServiceClientBase, ChatCompletionClientBase])
    assert service_get == kernel_with_service.services["service"]


def test_get_service_with_type_not_found(kernel_with_service: Kernel):
    with pytest.raises(ServiceInvalidTypeError):
        kernel_with_service.get_service("service", type=ChatCompletionClientBase)


def test_get_service_no_id(kernel_with_service: Kernel):
    service_get = kernel_with_service.get_service()
    assert service_get == kernel_with_service.services["service"]


def test_instantiate_prompt_execution_settings_through_kernel(kernel_with_service: Kernel):
    settings = kernel_with_service.get_prompt_execution_settings_from_service_id("service")
    assert settings.service_id == "service"


# endregion
