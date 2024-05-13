# Copyright (c) Microsoft. All rights reserved.
import sys
from typing import Any, AsyncGenerator, Iterable, Optional, Union

if sys.version_info >= (3, 9):
    from typing import Annotated
else:
    from typing_extensions import Annotated

import pytest

from semantic_kernel.connectors.ai.open_ai.services.open_ai_chat_completion import OpenAIChatCompletion
from semantic_kernel.exceptions import FunctionExecutionException, FunctionInitializationError
from semantic_kernel.functions.kernel_arguments import KernelArguments
from semantic_kernel.functions.kernel_function import KernelFunction
from semantic_kernel.functions.kernel_function_decorator import kernel_function
from semantic_kernel.kernel import Kernel
from semantic_kernel.kernel_pydantic import KernelBaseModel


def test_init_native_function_with_input_description():
    @kernel_function(description="Mock description", name="mock_function")
    def mock_function(input: Annotated[str, "input"], arguments: "KernelArguments") -> None:
        pass

    mock_method = mock_function

    native_function = KernelFunction.from_method(method=mock_method, plugin_name="MockPlugin")

    assert native_function.method == mock_method
    assert native_function.parameters[0].name == "input"
    assert native_function.parameters[0].description == "input"
    assert not native_function.parameters[0].default_value
    assert native_function.parameters[0].type_ == "str"
    assert native_function.parameters[0].is_required is True
    assert native_function.parameters[1].name == "arguments"
    assert native_function.parameters[1].description == ""
    assert not native_function.parameters[1].default_value
    assert native_function.parameters[1].type_ == "KernelArguments"
    assert native_function.parameters[1].is_required is True


def test_init_native_function_without_input_description():
    @kernel_function()
    def mock_function(arguments: "KernelArguments") -> None:
        pass

    mock_function.__kernel_function__ = True
    mock_function.__kernel_function_name__ = "mock_function_no_input_desc"
    mock_function.__kernel_function_description__ = "Mock description no input desc"
    mock_function.__kernel_function_parameters__ = [
        {
            "name": "arguments",
            "description": "Param 1 description",
            "default_value": "default_param1_value",
            "is_required": True,
        }
    ]

    mock_method = mock_function

    native_function = KernelFunction.from_method(method=mock_method, plugin_name="MockPlugin")

    assert native_function.method == mock_method
    assert native_function.parameters[0].name == "arguments"
    assert native_function.parameters[0].description == "Param 1 description"
    assert native_function.parameters[0].default_value == "default_param1_value"
    assert native_function.parameters[0].type_ == "str"
    assert native_function.parameters[0].is_required is True


def test_init_native_function_from_kernel_function_decorator():
    @kernel_function(
        description="Test description",
        name="test_function",
    )
    def decorated_function(input: Annotated[Optional[str], "Test input description"] = "test_default_value") -> None:
        pass

    assert decorated_function.__kernel_function__ is True
    assert decorated_function.__kernel_function_description__ == "Test description"
    assert decorated_function.__kernel_function_name__ == "test_function"

    native_function = KernelFunction.from_method(method=decorated_function, plugin_name="MockPlugin")

    assert native_function.method == decorated_function
    assert native_function.parameters[0].name == "input"
    assert native_function.parameters[0].description == "Test input description"
    assert native_function.parameters[0].default_value == "test_default_value"
    assert native_function.parameters[0].type_ == "str"
    assert native_function.parameters[0].is_required is False


def test_init_native_function_from_kernel_function_decorator_defaults():
    @kernel_function()
    def decorated_function() -> None:
        pass

    assert decorated_function.__kernel_function__ is True
    assert decorated_function.__kernel_function_description__ is None
    assert decorated_function.__kernel_function_name__ == "decorated_function"

    native_function = KernelFunction.from_method(method=decorated_function, plugin_name="MockPlugin")

    assert native_function.method == decorated_function
    assert len(native_function.parameters) == 0


def test_init_method_is_none():
    with pytest.raises(FunctionInitializationError):
        KernelFunction.from_method(method=None, plugin_name="MockPlugin")


def test_init_method_is_not_kernel_function():
    def not_kernel_function():
        pass

    with pytest.raises(FunctionInitializationError):
        KernelFunction.from_method(method=not_kernel_function, plugin_name="MockPlugin")


def test_init_invalid_name():
    @kernel_function(name="invalid name")
    def invalid_name():
        pass

    with pytest.raises(FunctionInitializationError):
        KernelFunction.from_method(method=invalid_name, plugin_name="MockPlugin")


@pytest.mark.asyncio
async def test_invoke_non_async():
    @kernel_function()
    def non_async_function() -> str:
        return ""

    native_function = KernelFunction.from_method(method=non_async_function, plugin_name="MockPlugin")

    result = await native_function.invoke(kernel=None, arguments=None)
    assert result.value == ""

    async for partial_result in native_function.invoke_stream(kernel=None, arguments=None):
        assert isinstance(partial_result.metadata["exception"], NotImplementedError)


@pytest.mark.asyncio
async def test_invoke_async():
    @kernel_function()
    async def async_function() -> str:
        return ""

    native_function = KernelFunction.from_method(method=async_function, plugin_name="MockPlugin")

    result = await native_function.invoke(kernel=None, arguments=None)
    assert result.value == ""

    async for partial_result in native_function.invoke_stream(kernel=None, arguments=None):
        assert isinstance(partial_result.metadata["exception"], NotImplementedError)


@pytest.mark.asyncio
async def test_invoke_gen():
    @kernel_function()
    def gen_function() -> Iterable[str]:
        yield ""

    native_function = KernelFunction.from_method(method=gen_function, plugin_name="MockPlugin")

    result = await native_function.invoke(kernel=None, arguments=None)
    assert result.value == [""]

    async for partial_result in native_function.invoke_stream(kernel=None, arguments=None):
        assert partial_result == ""


@pytest.mark.asyncio
async def test_invoke_gen_async():
    @kernel_function()
    async def async_gen_function() -> AsyncGenerator[str, Any]:
        yield ""

    native_function = KernelFunction.from_method(method=async_gen_function, plugin_name="MockPlugin")

    result = await native_function.invoke(kernel=None, arguments=None)
    assert result.value == [""]

    async for partial_result in native_function.invoke_stream(kernel=None, arguments=None):
        assert partial_result == ""


@pytest.mark.asyncio
async def test_service_execution():
    kernel = Kernel()
    service = OpenAIChatCompletion(service_id="test", ai_model_id="test", api_key="test")
    req_settings = service.get_prompt_execution_settings_class()(service_id="test")
    req_settings.temperature = 0.5
    kernel.add_service(service)
    arguments = KernelArguments(settings=req_settings)

    @kernel_function(name="function")
    def my_function(kernel, service, execution_settings, arguments) -> str:
        assert kernel is not None
        assert isinstance(kernel, Kernel)
        assert service is not None
        assert isinstance(service, OpenAIChatCompletion)
        assert execution_settings is not None
        assert isinstance(execution_settings, req_settings.__class__)
        assert execution_settings.temperature == 0.5
        assert arguments is not None
        assert isinstance(arguments, KernelArguments)
        return "ok"

    func = KernelFunction.from_method(my_function, "test")

    result = await func.invoke(kernel, arguments)
    assert result.value == "ok"


@pytest.mark.asyncio
async def test_required_param_not_supplied():
    @kernel_function()
    def my_function(input: str) -> str:
        return input

    func = KernelFunction.from_method(my_function, "test")

    result = await func.invoke(kernel=None, arguments=KernelArguments())
    assert isinstance(result.metadata["exception"], FunctionExecutionException)


@pytest.mark.asyncio
async def test_service_execution_with_complex_object():
    kernel = Kernel()

    class InputObject(KernelBaseModel):
        arg1: str
        arg2: int

    @kernel_function(name="function")
    def my_function(input_obj: InputObject) -> str:
        assert input_obj is not None
        assert isinstance(input_obj, InputObject)
        assert input_obj.arg1 == "test"
        assert input_obj.arg2 == 5
        return f"{input_obj.arg1} {input_obj.arg2}"

    func = KernelFunction.from_method(my_function, "test")

    arguments = KernelArguments(input_obj=InputObject(arg1="test", arg2=5))
    result = await func.invoke(kernel, arguments)
    assert result.value == "test 5"


class InputObject(KernelBaseModel):
    arg1: str
    arg2: int


@pytest.mark.asyncio
async def test_service_execution_with_complex_object_from_str():
    kernel = Kernel()

    @kernel_function(name="function")
    def my_function(input_obj: InputObject) -> str:
        assert input_obj is not None
        assert isinstance(input_obj, InputObject)
        assert input_obj.arg1 == "test"
        assert input_obj.arg2 == 5
        return f"{input_obj.arg1} {input_obj.arg2}"

    func = KernelFunction.from_method(my_function, "test")

    arguments = KernelArguments(input_obj={"arg1": "test", "arg2": 5})
    result = await func.invoke(kernel, arguments)
    assert result.value == "test 5"


@pytest.mark.asyncio
async def test_service_execution_with_complex_object_from_str_mixed():
    kernel = Kernel()

    @kernel_function(name="function")
    def my_function(input_obj: InputObject, input_str: str) -> str:
        assert input_obj is not None
        assert isinstance(input_obj, InputObject)
        assert input_obj.arg1 == "test"
        assert input_obj.arg2 == 5
        return f"{input_obj.arg1} {input_str} {input_obj.arg2}"

    func = KernelFunction.from_method(my_function, "test")

    arguments = KernelArguments(input_obj={"arg1": "test", "arg2": 5}, input_str="test2")
    result = await func.invoke(kernel, arguments)
    assert result.value == "test test2 5"


@pytest.mark.asyncio
async def test_service_execution_with_complex_object_from_str_mixed_multi():
    kernel = Kernel()

    @kernel_function(name="function")
    def my_function(input_obj: InputObject, input_str: Union[str, int]) -> str:
        assert input_obj is not None
        assert isinstance(input_obj, InputObject)
        assert input_obj.arg1 == "test"
        assert input_obj.arg2 == 5
        return f"{input_obj.arg1} {input_str} {input_obj.arg2}"

    func = KernelFunction.from_method(my_function, "test")

    arguments = KernelArguments(input_obj={"arg1": "test", "arg2": 5}, input_str="test2")
    result = await func.invoke(kernel, arguments)
    assert result.value == "test test2 5"
