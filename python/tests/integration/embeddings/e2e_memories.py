import semantic_kernel as sk


async def simple_memory_test(kernel: sk.Kernel):
    # Add some documents to the semantic memory
    await kernel.memory.save_information_async(
        "test", id="info1", text="Sharks are fish."
    )
    await kernel.memory.save_information_async(
        "test", id="info2", text="Whales are mammals."
    )
    await kernel.memory.save_information_async(
        "test", id="info3", text="Penguins are birds."
    )
    await kernel.memory.save_information_async(
        "test", id="info4", text="Dolphins are mammals."
    )
    await kernel.memory.save_information_async(
        "test", id="info5", text="Flies are insects."
    )

    # Search for documents
    query = "What are mammals?"
    result = await kernel.memory.search_async(
        "test", query, limit=2, min_relevance_score=0.0
    )
    print(f"Query: {query}")
    print(f"\tAnswer 1: {result[0].text}")
    print(f"\tAnswer 2: {result[1].text}\n")
    assert "mammals." in result[0].text
    assert "mammals." in result[1].text

    query = "What are fish?"
    result = await kernel.memory.search_async(
        "test", query, limit=1, min_relevance_score=0.0
    )
    print(f"Query: {query}")
    print(f"\tAnswer: {result[0].text}\n")
    assert result[0].text == "Sharks are fish."

    query = "What are insects?"
    result = await kernel.memory.search_async(
        "test", query, limit=1, min_relevance_score=0.0
    )
    print(f"Query: {query}")
    print(f"\tAnswer: {result[0].text}\n")
    assert result[0].text == "Flies are insects."

    query = "What are birds?"
    result = await kernel.memory.search_async(
        "test", query, limit=1, min_relevance_score=0.0
    )
    print(f"Query: {query}")
    print(f"\tAnswer: {result[0].text}\n")
    assert result[0].text == "Penguins are birds."
