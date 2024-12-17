[Getting Started](#getting-started)

# The Data Problem You Knew Was Coming
### Shortcuts and Technical Debt
Most greenfield projects never start out with the intent to include bad design. Experienced engineers have already spent way too much time dealing with the problems left behind by others. This time, it's going to be different! 
Often, however, what starts out as a small microservice working with occasional updates, quickly morphs into a data ingesting monster in a matter of months. No one wants to create the monster.
But time pressure is a huge part of business. Sometimes the deadline just can't be moved. When that happens, the engineer is forced to make compromises. If there were more time, maybe there could have been some research spikes done to figure out the best database to use. Data streaming is clearly becoming a standard but it can take time to build the right design from the ground up to support it. But shortcuts had to be taken, and maybe some tests had to be skipped. 
All of this adds up to technical debt. Tech debt is just like credit, buy now, pay later. Sometimes it can be ignored, but eventually, it starts to slow the development process down. Bugs start to take over the whole release cycle.Before you know it, there's almost no time for new features. In an article written by [Sonar](https://www.sonarsource.com/blog/new-research-from-sonar-on-cost-of-technical-debt/), a study made on 200 different projects revealed the annual cost for a project with one million lines of code (LoC) is $306,000. That is the equivalent of 5,500 hours of engineering time.

### Data Explosion
With the advent of Big Data and AI, our world is experiencing an explosion of data. In an article published by [G2](https://www.g2.com/articles/big-data-statistics), it is estimated that in 2025, the world will produce 463 ZB (Zeta Bytes, that's 463,000,000,000,000,000,000,000 [21 zeros] Bytes) every day at a worth of over $220 billion. 

Every 60 seconds .. 
![The Data Explosion](docs/images/Internet-in-60-Seconds.drawio.png "The Data Explosion")

The average company now wrangles with _400_ different data sources!


## Who is IntraCel For?
In the end, software is really about helping people solve real-world problems. 
IntraCel is a Clojure library that's meant to help engineers have powerful functional tools to make managing those problems easier. Clojure is functional, flexible, and fast and fits most software needs like a glove. 

The development team behind IntraCel has been developing in Clojure for almost a decade and wants to share the love. That's why the heart of IntraCel will always remain open source. We want the Clojure community to expand. Others need to see what they've been missing! 

IntraCel is an approachable library that encourages good design and lets you, the architect, keep your options open so that you can have the flexibility you need to grow your tech stack without the library getting in the way.

## All Design Decisions Come with a Cost
[![Reducing The Cost of Being Wrong](https://img.youtube.com/vi/RHbZk4qGazE/0.jpg)](https://www.youtube.com/watch?v=RHbZk4qGazE)

### Red Pill or Blue Pill?
In the movie, The Matrix, Neo was given a choice. The blue pill would let him go back to his regular life like nothing was wrong. The red pill would show him the truth he wouldn't be able to unsee.
Unfortunately, for software engineers building modern software, the blue pill means just avoiding the inevitable deluge of data. Software that isn't designed to support data-intensive tasks will have to deal with redesigning architecture **_AFTER_** it's been released. 

Kent Beck, one of the creators of Agile, explains that the initial investment in building software pales in comparison to the cost of maintaining it. He explains that the cost to maintain it is directly related to the cost of coupling in the system. That is when a change to one component cascades a change to another component, which would cascade to another component, and so on. 

IntraCel engineers have taken the red pill. The concepts built into the library believe that good software design embraces decoupling wherever possible. While it comes with out-of-the-box capabilities built on embedded systems, it uses protocols and interfaces to decouple design from implementations. This makes it simpler to change over time and encourages good practices. IntraCel leverages Clojure's innate capabilities of concurrency and unmatched processing power to do data-intensive tasks quickly and reliably.

## Getting Started
Here's how to get started with IntraCel/core

<!--## Good Design Is About Planning Ahead for Unavoidable Growth

# IntraCel Arms You With Years of Architectural Experience

## IntraCel Prepares You For the Data Explosion

## IntraCel Is Built By Engineers Who Care About Cost -->


