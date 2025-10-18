SQL query parser

In SQL, the most syntactically complex and tricky query is probably the SELECT query. It has explicit and implicit joins, groupings,
subqueries, sorting and truncation of selects - all this beauty can occur repeatedly even in one single
select query.

For example, like this:
```sql
SELECT * FROM book
```

Would be parsed into an AST like this:
```python
Select(
  expressions=[
    Star()],
  from=From(
    node=Table(
      node=Identifier(node=book, quoted=false))))
```
Or something slightly more complex like this:
```sql
SELECT author.name, count(book.id), sum(book.cost) 
FROM author 
LEFT JOIN book ON (author.id = book.author_id) 
GROUP BY author.name 
HAVING COUNT(*) > 1 AND SUM(book.cost) > 500
LIMIT 10;
```

Would be represented as:
```python
Select(
  expressions=[
    Column(
      node=Identifier(node=name, quoted=false),
      table=Identifier(node=author, quoted=false)),
    FunctionCall(
      args=[
        Column(
          node=Identifier(node=id, quoted=false),
          table=Identifier(node=book, quoted=false))],
      node=count),
    FunctionCall(
      args=[
        Column(
          node=Identifier(node=cost, quoted=false),
          table=Identifier(node=book, quoted=false))],
      node=sum)],
  from=From(
    node=Table(
      node=Identifier(node=author, quoted=false))),
  group=Group(
    expressions=[
      Column(
        node=Identifier(node=name, quoted=false),
        table=Identifier(node=author, quoted=false))]),
  having=Having(
    node=And(
      left=GT(
        left=FunctionCall(
          args=[
            Star()],
          node=COUNT),
        right=NumericLiteral(value=1)),
      right=GT(
        left=FunctionCall(
          args=[
            Column(
              node=Identifier(node=cost, quoted=false),
              table=Identifier(node=book, quoted=false))],
          node=SUM),
        right=NumericLiteral(value=500)))),
  joins=[
    Join(
      node=Table(
        node=Identifier(node=book, quoted=false)),
      on=EQ(
        left=Column(
          node=Identifier(node=id, quoted=false),
          table=Identifier(node=author, quoted=false)),
        right=Column(
          node=Identifier(node=author_id, quoted=false),
          table=Identifier(node=book, quoted=false))),
      side=LEFT)],
  limit=Limit(
    node=NumericLiteral(value=10)))
```

What constructs the parser supports:
- Enumeration of sample fields explicitly (with aliases) or *
- Implicit join of several tables (select * from A,B,C)
- Explicit join of tables (inner, left, right, full join)
- Filter conditions (where a = 1 and b > 100)
- Subqueries (select * from (select * from A) a_alias)
- Grouping by one or several fields (group by)
- Sorting by one or more fields (order by)
- Selection truncation (limit, offset)